package org.gridkit.nimble.driver;

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.gridkit.nimble.driver.ExecutionDriver.ExecutionConfig;
import org.gridkit.nimble.driver.ExecutionDriver.ExecutionObserver;
import org.gridkit.nimble.driver.ExecutionDriver.IterationLimit;
import org.gridkit.nimble.driver.ExecutionDriver.RateLimitedRun;
import org.gridkit.nimble.metering.SpanSampler;
import org.gridkit.nimble.statistics.TimeUtils;
import org.gridkit.util.concurrent.Barriers;
import org.gridkit.util.concurrent.BlockingBarrier;

public class ExecutionHelper {

	public static ExecutionDriver newDriver() {
		return new ExecDriver();
	}
	
	public static ExecutionConfig unlimitedExecution(int totalThreads, boolean perNode) {
		if (perNode) {
			return new ThreadCountConfig(totalThreads);
		}
		else {
			throw new UnsupportedOperationException("Not done yet");
		}				
	}

	public static ExecutionConfig constantRateExecution(double rate, int totalThreads, boolean perNode) {
		if (perNode) {
			return new ConstantRateConfig(rate, totalThreads);
		}
		else {
			throw new UnsupportedOperationException("Not done yet");
		}		
	}
	
	private static class ExecDriver implements ExecutionDriver, Serializable {

		private static final long serialVersionUID = 20121017L;

		@Override
		public Activity start(Runnable task, ExecutionConfig config, MeteringSink<SpanSampler> sampler) {
			Run run = new Run();
			run.task = task;
			run.threadCount = config.getThreadCount();

			if (sampler != null) {
				run.observer = new MeteringObserver(sampler.getSink());
			}
			
			if (config instanceof RateLimitedRun) {
				run.iterationBarrier = ((RateLimitedRun) config).getRateLimiter();
			}

			run.start();
			
			return run;
		}		
	}
	
	private static class Run implements Activity, Runnable {
		
		int threadCount;
		IterationLimit iterationLimit;
		BlockingBarrier iterationBarrier;
		MeteringObserver observer;
		Runnable task;
		boolean stopOnTaskError = true;

		private ExecutorService service;
		private volatile boolean stopped;
		private Exception lastError;
		private int realThreadCount;
		private CountDownLatch runLatch = new CountDownLatch(1);
		
		
		void start() {
			service = Executors.newCachedThreadPool();
			startWorkers(threadCount);
		}
		
		private synchronized void startWorkers(int threads) {
			if (threads > 0) { 
				realThreadCount += threads;
				for(int i = 0; i != threads; ++i) {
					service.submit(this);
				}
			}
			else {
				service.submit(this);
			}
		}

		@Override
		public void run() {
			try {
				while(!stopped) {
					if (iterationLimit != null && !iterationLimit.hasMoreIteration()) {
						stop();
						break;
					}
					if (iterationBarrier != null) {
						iterationBarrier.pass();
					}
					if (stopped) {
						break;
					}
					
					long st = System.nanoTime();
					Exception e = null;
					try {
						task.run();
					}
					catch(Exception ee) {
						e = ee;
					}
					long fn = System.nanoTime();
					
					if (observer != null) {
						observer.done(st, fn, e);
					}
					if (e != null && stopOnTaskError) {
						lastError = e;
						stopped = true;						
					}					
				}
			}
			catch(Exception e) {
				stop();
				lastError = e;
			}
			synchronized(this) {
				--realThreadCount;
				if (realThreadCount <= 0) {
					runLatch.countDown();
					service.shutdown();
				}
			}
		}

		@Override
		public void stop() {
			stopped = true;			
		}

		@Override
		public void join() {
			try {
				runLatch.await();
				if (lastError != null) {
					throw new RuntimeException(lastError);
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static class MeteringObserver implements ExecutionObserver {

		private final SpanSampler sampler;
		
		public MeteringObserver(SpanSampler sampler) {
			this.sampler = sampler;
		}

		@Override
		public void done(long startNanos, long finishNanos, Throwable exception) {
			sampler.write(TimeUtils.toSeconds(finishNanos - startNanos), startNanos, finishNanos);
		}
	}
	
	private static class ThreadCountConfig implements ExecutionDriver.ExecutionConfig, Serializable {
		
		private static final long serialVersionUID = 20121017L;

		private final int threadLimit;

		public ThreadCountConfig(int threadLimit) {
			this.threadLimit = threadLimit;
		}

		@Override
		public int getThreadCount() {
			return threadLimit;
		}
	}
	
	private static class ConstantRateConfig implements ExecutionConfig, RateLimitedRun, Serializable {

		private static final long serialVersionUID = 20121017L;
		
		private final double rate;
		private final int threadLimit;
		
		public ConstantRateConfig(double rate, int threadLimit) {
			this.rate = rate;
			this.threadLimit = threadLimit;
		}

		@Override
		public BlockingBarrier getRateLimiter() {
			return Barriers.speedLimit(rate);
		}

		@Override
		public int getThreadCount() {
			return threadLimit;
		}
	}
}

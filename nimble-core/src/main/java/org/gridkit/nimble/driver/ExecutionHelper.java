package org.gridkit.nimble.driver;

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.gridkit.nimble.driver.ExecutionDriver.ExecutionConfig;
import org.gridkit.nimble.driver.ExecutionDriver.ExecutionObserver;
import org.gridkit.nimble.driver.ExecutionDriver.IterationLimit;
import org.gridkit.nimble.driver.ExecutionDriver.RateLimitedRun;
import org.gridkit.nimble.metering.MeteringTemplate;
import org.gridkit.nimble.metering.SampleFactory;
import org.gridkit.util.concurrent.Barriers;
import org.gridkit.util.concurrent.BlockingBarrier;

public class ExecutionHelper {

	public static ExecutionDriver newDriver() {
		return new ExecDriver();
	}
	
	public static ExecutionConfig constantRateExecution(double rate, int totalThreads, boolean perNode) {
		if (perNode) {
			return new ConstantRateConfig(rate, totalThreads);
		}
		else {
			throw new UnsupportedOperationException("Not done yet");
		}		
	}
	
	private static class ExecDriver implements ExecutionDriver {

		@Override
		public Activity start(Runnable task, ExecutionConfig config, MeteringDriver metering, MeteringTemplate sampleTemplate) {
			Run run = new Run();
			run.task = task;
			run.threadCount = config.getThreadCount();

			if (sampleTemplate != null) {
				run.observer = new MeteringObserver(sampleTemplate.createFactory(metering.getSchema()));
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
			++threadCount;
			for(int i = 0; i != threads; ++i) {
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
				boolean last = 0 == --realThreadCount;
				if (last) {
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

		private final SampleFactory factory;
		
		public MeteringObserver(SampleFactory factory) {
			this.factory = factory;
		}

		@Override
		public void done(long startNanos, long finishNanos, Throwable exception) {
			factory.newSample()
				.setTimeBounds(startNanos, finishNanos)
				.setMeasure((double)(finishNanos - startNanos) / TimeUnit.SECONDS.toNanos(1));
		}
	}
	
	private static class ConstantRateConfig implements ExecutionConfig, RateLimitedRun, Serializable {

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

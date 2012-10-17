package org.gridkit.nimble.driver;

import org.gridkit.nimble.metering.MeteringTemplate;
import org.gridkit.util.concurrent.BlockingBarrier;


public interface ExecutionDriver {

	public Activity start(Runnable task, ExecutionConfig execConfig, MeteringDriver driver, MeteringTemplate sampleTemplate);	
	
	interface ExecutionConfig {
	
		public int getThreadCount();
		
	}
	
	interface RateLimitedRun extends ExecutionConfig {
		
		public BlockingBarrier getRateLimiter();
		
	}
	
	interface ExecutionObserver {
		
		public void done(long startNanos, long finishNanos, Throwable exception);
		
	}
	
	interface IterationLimit {

		/**
		 * Called before each iteration.  
		 * @return <code>true</code> if should proceed
		 */
		public boolean hasMoreIteration();
		
	}
}

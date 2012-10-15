package org.gridkit.nimble.task;

public interface BackgroundTask<V extends CommonContext> {

	public void init(V context);
	
	/**
	 * Called at beginning of run phase.
	 */
	public void start();

	/**
	 * Called then run phase is completed. Blocks until complete
	 * stop of task.
	 */
	public void stop();
	
	/**
	 * Called then abnormal termination of run phase is requested.
	 */
	public void abort();
	
}

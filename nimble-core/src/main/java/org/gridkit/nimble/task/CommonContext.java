package org.gridkit.nimble.task;

import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.util.concurrent.TaskService;

public interface CommonContext {

	public SampleSchema getMetering();
	
	public TaskService getTaskService();
	
	public void abort(Exception e);
}

package org.gridkit.nimble.metering;

public interface TimeReporter {

	public StopWatch start();
	
	public interface StopWatch {
		
		public void stop();
		
	}	
}

package org.gridkit.nimble.metering;


public interface SpanReporter {
	
	public StopWatch start();
	
	public interface StopWatch {

		public void stop(double measure);
		
	}
}

package org.gridkit.nimble.metering;

public interface DTimeReporter<T extends Enum<T>> {

	public StopWatch<T> start();
	
	public interface StopWatch<T extends Enum<T>> {
		
		public void stop(T descriminator);
		
	}	
}

package org.gridkit.nimble.metering;

public interface SimpleSampler {

	public interface Scalar {
		
		public void write(double value);
		
	}
	
	public interface Event {

		public void write(long nanotimestamp, double value);
		
	}

	public interface Interval {

		public void write(long nanoStart, long nanoEnd, double value);
		
	}
	
}

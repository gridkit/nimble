package org.gridkit.nimble.metering;

public interface SampleFactory {

	public SampleWriter newSample();
	
	public void trace(boolean enable);
	
}

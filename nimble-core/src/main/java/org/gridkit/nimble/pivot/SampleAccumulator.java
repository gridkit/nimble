package org.gridkit.nimble.pivot;

import org.gridkit.nimble.metering.SampleReader;

public interface SampleAccumulator {

	public void accumulate(SampleReader samples);
	
	public void flush();
	
}

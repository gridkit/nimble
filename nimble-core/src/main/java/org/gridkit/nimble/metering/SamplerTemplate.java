package org.gridkit.nimble.metering;

public interface SamplerTemplate<V> {

	public V createSampler(SampleSchema schema);
	
}

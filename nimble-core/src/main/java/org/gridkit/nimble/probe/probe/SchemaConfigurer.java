package org.gridkit.nimble.probe.probe;

import org.gridkit.nimble.metering.SampleSchema;

public interface SchemaConfigurer<T> {
	
	public SampleSchema configure(T target, SampleSchema root);
	
}

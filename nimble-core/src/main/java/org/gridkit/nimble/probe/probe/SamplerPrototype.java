package org.gridkit.nimble.probe.probe;

import org.gridkit.nimble.metering.SampleSchema;

public interface SamplerPrototype<S> {
	
	public S instantiate(SampleSchema schema);

}

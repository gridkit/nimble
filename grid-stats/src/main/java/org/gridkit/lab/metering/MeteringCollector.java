package org.gridkit.lab.metering;

public interface MeteringCollector {
	
	public <O extends Observed, S extends Sampler> SamplerProvider<O, S> bind(SamplerHandle<S> handle); 

	public <O extends Observed, S extends Sampler> SamplerProvider<O, S> bind(Class<O> observed, SamplerHandle<S> handle); 

}

package org.gridkit.lab.metering;

public interface SamplerProvider<O extends Observed, S extends Sampler> {

	public S getSampler(O observed);
	
}

package org.gridkit.lab.monitoring.probe;


public interface PollProbeDeployer<T, S> {
	
	public PollProbe deploy(T target, SamplerProvider<T, S> provider);

}

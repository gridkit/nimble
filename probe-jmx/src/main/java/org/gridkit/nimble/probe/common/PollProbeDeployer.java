package org.gridkit.nimble.probe.common;

public interface PollProbeDeployer<T, S> {
	
	public PollProbe deploy(T target, SamplerProvider<T, S> provider);

}

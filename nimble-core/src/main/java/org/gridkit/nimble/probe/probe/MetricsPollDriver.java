package org.gridkit.nimble.probe.probe;

import org.gridkit.nimble.driver.Activity;
import org.gridkit.nimble.probe.common.PollProbeDeployer;
import org.gridkit.nimble.probe.common.TargetLocator;

public interface MetricsPollDriver {

	public <T, S> Activity deploy(TargetLocator<T> locator, PollProbeDeployer<T, S> deployer, SchemaConfigurer<T> schConfig, SamplerPrototype<S> sampleProto, long periodMs);
	
}

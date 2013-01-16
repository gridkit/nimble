package org.gridkit.nimble.probe.probe;

import org.gridkit.lab.monitoring.probe.PollProbeDeployer;
import org.gridkit.lab.monitoring.probe.TargetLocator;
import org.gridkit.nimble.driver.Activity;

public interface MonitoringDriver {

	public <T, S> Activity deploy(TargetLocator<T> locator, PollProbeDeployer<T, S> deployer, SchemaConfigurer<T> schConfig, SamplerPrototype<S> sampleProto, long periodMs);
	
}

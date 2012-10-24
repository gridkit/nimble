package org.gridkit.nimble.probe.jmx;

import org.gridkit.nimble.driver.Activity;
import org.gridkit.nimble.driver.MeteringSink;

public interface JmxPollProbe {

	public void setMBeanConnector(MBeanConnector connetor);
	
	public Activity addSampler(MeteringSink<JmxAwareSamplerProvider<Runnable>> genericSampler);
	
}

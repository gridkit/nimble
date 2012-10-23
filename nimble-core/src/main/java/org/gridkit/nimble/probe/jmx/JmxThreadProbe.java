package org.gridkit.nimble.probe.jmx;

import org.gridkit.nimble.driver.Activity;
import org.gridkit.nimble.driver.MeteringSink;

public interface JmxThreadProbe {

	public void setMBeanConnector(MBeanConnector connetor);
	
	public Activity addSampler(MeteringSink<JmxAwareSamplerProvider<JavaThreadStatsSampler>> samplerProvider);
	
}

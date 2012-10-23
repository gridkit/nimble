package org.gridkit.nimble.probe.jmx;

import javax.management.MBeanServerConnection;

public interface JmxAwareSamplerProvider<T> {
	
	public T getSampler(MBeanServerConnection connection);

}

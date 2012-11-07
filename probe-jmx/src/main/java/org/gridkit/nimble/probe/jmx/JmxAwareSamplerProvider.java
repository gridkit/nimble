package org.gridkit.nimble.probe.jmx;

import javax.management.MBeanServerConnection;

import org.gridkit.nimble.probe.common.SamplerProvider;

public interface JmxAwareSamplerProvider<T> extends SamplerProvider<MBeanServerConnection, T>{
	
	public T getSampler(MBeanServerConnection connection);

}

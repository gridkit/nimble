package org.gridkit.nimble.probe.jmx;

import javax.management.MBeanServerConnection;

import org.gridkit.lab.monitoring.probe.SamplerProvider;

public interface JmxAwareSamplerProvider<T> extends SamplerProvider<MBeanServerConnection, T>{
	
	public T getSampler(MBeanServerConnection connection);

}

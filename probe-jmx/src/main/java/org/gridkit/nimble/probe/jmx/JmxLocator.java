package org.gridkit.nimble.probe.jmx;

import java.io.Serializable;
import java.util.Collection;

import javax.management.MBeanServerConnection;

import org.gridkit.lab.monitoring.probe.TargetLocator;

public class JmxLocator implements TargetLocator<MBeanServerConnection>, Serializable {

	private static final long serialVersionUID = 20121106L;
	
	private final MBeanConnector connector;
	
	public JmxLocator(MBeanConnector connector) {
		this.connector = connector;
	}

	@Override
	public Collection<MBeanServerConnection> findTargets() {
		return connector.connect();
	}
	
	@Override
	public String toString() {
		return connector == null ? "null" : connector.toString();
	}
}

package org.gridkit.nimble.probe.jmx;

import java.util.Collection;

import javax.management.MBeanServerConnection;

public interface MBeanConnector {
	
	public Collection<MBeanServerConnection> connect();

}

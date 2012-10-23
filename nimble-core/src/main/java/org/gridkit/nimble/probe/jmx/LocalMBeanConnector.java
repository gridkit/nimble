package org.gridkit.nimble.probe.jmx;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Collections;

import javax.management.MBeanServerConnection;

public class LocalMBeanConnector implements MBeanConnector, Serializable {

	private static final long serialVersionUID = 20121023;

	@Override
	public Collection<MBeanServerConnection> connect() {
		MBeanServerConnection connection = ManagementFactory.getPlatformMBeanServer();
		return Collections.singleton(connection);
	}
}

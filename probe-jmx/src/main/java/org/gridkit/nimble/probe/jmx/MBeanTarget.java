package org.gridkit.nimble.probe.jmx;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

public class MBeanTarget {

	private final MBeanServerConnection connection;
	private final ObjectName mbeanName;
	
	public MBeanTarget(MBeanServerConnection connection, ObjectName mbeanName) {
		this.connection = connection;
		this.mbeanName = mbeanName;
	}

	public MBeanServerConnection getConnection() {
		return connection;
	}

	public ObjectName getMbeanName() {
		return mbeanName;
	}
	
	@Override
	public String toString() {
		return mbeanName.toString();
	}
}

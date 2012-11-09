package org.gridkit.nimble.probe.jmx;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.gridkit.lab.util.jmx.mxstruct.common.RuntimeMXStruct;

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

	public String getProcessName() {
		try {
			return (String) connection.getAttribute(RuntimeMXStruct.NAME, "Name");
		} catch (Exception e) {
			return connection.toString();
		}
	}
	
	public ObjectName getMbeanName() {
		return mbeanName;
	}
	
	@Override
	public String toString() {
		return mbeanName.toString();
	}
}

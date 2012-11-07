package org.gridkit.nimble.probe.jmx;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

public interface MBeanSampler {

	public void report(MBeanServerConnection connection, ObjectName target);
	
}

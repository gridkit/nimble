package org.gridkit.nimble.probe.jmx;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.management.MBeanServerConnection;

import org.gridkit.lab.jvm.attach.AttachManager;
import org.gridkit.lab.jvm.attach.JavaProcessId;
import org.gridkit.lab.jvm.attach.JavaProcessMatcher;

public class AttachMBeanConnector implements MBeanConnector, Serializable {

	private static final long serialVersionUID = 20121027L;
	
	private final JavaProcessMatcher processMatcher;
	
	public AttachMBeanConnector(JavaProcessMatcher processMatcher) {
		this.processMatcher = processMatcher;
	}

	@Override
	public Collection<MBeanServerConnection> connect() {
		List<MBeanServerConnection> result = new ArrayList<MBeanServerConnection>();
		for(JavaProcessId id: AttachManager.listJavaProcesses(processMatcher)) {
			MBeanServerConnection conn = AttachManager.getJmxConnection(id);
			if (conn != null) {
				result.add(conn);
			}
		}
		return result;
	}
}

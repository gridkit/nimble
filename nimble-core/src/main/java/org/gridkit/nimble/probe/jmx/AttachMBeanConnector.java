package org.gridkit.nimble.probe.jmx;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.management.MBeanServerConnection;

import org.gridkit.nimble.sensor.JvmMatcher;
import org.gridkit.nimble.util.JvmOps;

import com.sun.tools.attach.VirtualMachineDescriptor;

public class AttachMBeanConnector implements MBeanConnector, Serializable {

	private static final long serialVersionUID = 20121027L;
	
	private final JvmMatcher processMatcher;
	
	public AttachMBeanConnector(JvmMatcher processMatcher) {
		this.processMatcher = processMatcher;
	}

	@Override
	public Collection<MBeanServerConnection> connect() {
		List<MBeanServerConnection> result = new ArrayList<MBeanServerConnection>();
		for(VirtualMachineDescriptor vmd: JvmOps.listVms(processMatcher)) {
			result.add(JvmOps.getMBeanConnection(vmd));
		}
		return result;
	}
}

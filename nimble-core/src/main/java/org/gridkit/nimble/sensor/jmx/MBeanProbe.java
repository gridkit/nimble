package org.gridkit.nimble.sensor.jmx;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.gridkit.nimble.sensor.PidProvider;
import org.gridkit.nimble.sensor.Sensor;
import org.gridkit.nimble.util.JvmOps;

import com.sun.tools.attach.VirtualMachineDescriptor;

@SuppressWarnings("restriction")
public class MBeanProbe implements Sensor<Map<Long, Object>>, Serializable {

	private static final long serialVersionUID = 20121005L;
	
	private final PidProvider pidProvider;
	private final ObjectName mbeanName;
	private final String attrName;
	private transient Map<Long, MBeanServerConnection> connections;
	
	public MBeanProbe(PidProvider pidProvider, ObjectName mbeanName, String attrName) {
		this.pidProvider = pidProvider;
		this.mbeanName = mbeanName;
		this.attrName = attrName;
	}

	@Override
	public Map<Long, Object> measure() throws InterruptedException {
		ensureConnections();
		Map<Long, Object> map = new HashMap<Long, Object>();
		
		for(long pid: connections.keySet()) {
			try {
				MBeanServerConnection conn = connections.get(pid);
				Object val = conn.getAttribute(mbeanName, attrName);
				map.put(pid, val);
			}
			catch(Exception e) {
				/*
					System.err.println("PID: " + pid + "bean: " + mbeanName + " attr: " + attrName + " Error:" + e.toString());
				*/
				// ignore
			}
		}
		
		return map;
	}

	private void ensureConnections() {
		if (connections == null) {
			connections = new HashMap<Long, MBeanServerConnection>();
			Set<Long> pids = new HashSet<Long>(pidProvider.getPids());
			for(VirtualMachineDescriptor vm: JvmOps.listVms()) {
				long pid = Long.valueOf(vm.id());
				if (pids.contains(pid)) {
					try {
						MBeanServerConnection conn = JvmOps.getMBeanConnection(vm);
						connections.put(pid, conn);
					}
					catch(Exception e) {
						// ignore
					}
				}
			}
//			System.out.println("MBeanProbe ready: " + mbeanName + "#" + attrName);
		}
	}
	
//	private void readObject(ObjectInputStream is) throws IOException, ClassNotFoundException {
//		is.defaultReadObject();
//		System.out.println("Read object: " + mbeanName + "#" + attrName);
//		ensureConnections();
//	}
	
	@Override
	public String toString() {
		return "MBanProbe:" + mbeanName + "#" + attrName;
	}
}

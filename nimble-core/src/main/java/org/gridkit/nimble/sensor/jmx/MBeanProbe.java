package org.gridkit.nimble.sensor.jmx;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.gridkit.lab.jvm.attach.AttachManager;
import org.gridkit.lab.jvm.attach.JavaProcessId;
import org.gridkit.nimble.probe.PidProvider;
import org.gridkit.nimble.sensor.Sensor;

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
			for(JavaProcessId jpid: AttachManager.listJavaProcesses()) {
				long pid = jpid.getPID();
				if (pids.contains(pid)) {
					try {
						connections.put(pid, AttachManager.getDetails(jpid).getMBeans());								
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

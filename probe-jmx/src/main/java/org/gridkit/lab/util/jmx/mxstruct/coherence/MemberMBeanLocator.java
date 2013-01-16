package org.gridkit.lab.util.jmx.mxstruct.coherence;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.gridkit.lab.monitoring.probe.TargetLocator;
import org.gridkit.nimble.probe.jmx.MBeanConnector;
import org.gridkit.nimble.probe.jmx.MBeanTarget;

public class MemberMBeanLocator implements TargetLocator<MBeanTarget>, Serializable {
	
	private static final long serialVersionUID = 20121108L;
	
	private final MBeanConnector connector;
	private final ObjectName query;
	
	public MemberMBeanLocator(MBeanConnector connector, ObjectName query) {
		this.connector = connector;
		this.query = query;
	}

	@Override
	public Collection<MBeanTarget> findTargets() {
		List<MBeanTarget> result = new ArrayList<MBeanTarget>();
		for(MBeanServerConnection conn: connector.connect()) {
			try {
				int localMemberId = ClusterMXStruct.get(conn).getLocalMemberId();
				for(ObjectName name: conn.queryNames(query, null)) {
					String nodeId = name.getKeyProperty("nodeId");
					if (nodeId == null || nodeId.equals(String.valueOf(localMemberId))) {
						result.add(new MBeanTarget(conn, name));
					}
				}
			} catch (Exception e) {
				// TODO logging
				// ignore
			}
		}
		return result;
	}
}

package org.gridkit.nimble.probe.jmx;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.gridkit.nimble.probe.common.TargetLocator;

public class MBeanLocator implements TargetLocator<MBeanTarget>, Serializable {
		
	private static final long serialVersionUID = 20121106L;
	
	private final MBeanConnector connector;
	private final ObjectName query;
	
	public MBeanLocator(MBeanConnector connector, ObjectName query) {
		this.connector = connector;
		this.query = query;
	}

	@Override
	public Collection<MBeanTarget> findTargets() {
		List<MBeanTarget> result = new ArrayList<MBeanTarget>();
		for(MBeanServerConnection conn: connector.connect()) {
			try {
				for(ObjectName name: conn.queryNames(query, null)) {
					result.add(new MBeanTarget(conn, name));
				}
			} catch (IOException e) {
				// TODO logging
				// ignore
			}
		}
		return result;
	}
}

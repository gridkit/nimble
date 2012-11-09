package org.gridkit.nimble.probe.jmx.mxstruct;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.management.ReflectionException;

import org.gridkit.lab.util.jmx.mxstruct.coherence.ConnectionManagerMXStruct;
import org.gridkit.lab.util.jmx.mxstruct.coherence.MemberMBeanLocator;
import org.gridkit.nimble.probe.jmx.MBeanTarget;
import org.gridkit.nimble.probe.jmx.RemoteMBeanConnector;
import org.junit.Test;

public class CoherenceMBeanCheck {
	
	@Test
	public void find_connection_managers() throws ReflectionException, IOException {
		
		List<String> hosts = Arrays.asList(new String[]{"longmrdfappd1.uk.db.com", "longmrdfappd2.uk.db.com", "longmrdfappd3.uk.db.com"});
		RemoteMBeanConnector connector = new RemoteMBeanConnector(hosts, 7300, 7301, 7302, 7303);
		MemberMBeanLocator locator = new MemberMBeanLocator(connector, ConnectionManagerMXStruct.NAME);
		for(MBeanTarget mt: locator.findTargets()) {
			System.out.println(mt.getProcessName() + " " + mt.getMbeanName());
			ConnectionManagerMXStruct conMan = ConnectionManagerMXStruct.PROTO.read(mt.getConnection(), mt.getMbeanName());
			System.out.println(conMan.toString());
		}		
	}
}

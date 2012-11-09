package org.gridkit.lab.util.jmx.mxstruct.coherence;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.gridkit.lab.util.jmx.mxstruct.MXStruct;

public class ClusterMXStruct extends MXStruct {

	public static ObjectName NAME = name("Coherence:type=Cluster");
	public static ClusterMXStruct PROTO = new ClusterMXStruct();

	public static ClusterMXStruct get(MBeanServerConnection conn) throws ReflectionException, IOException {
		return PROTO.read(conn, NAME);
	}
	
	@AttrName("ClusterName")
	public String getClusterName() {
		return super.getMXAttr();
	}

	@AttrName("LocalMemberId")
	public int getLocalMemberId() {
		return super.getMXAttr();
	}

	@AttrName("ClusterSize")
	public int getClusterSize() {
		return super.getMXAttr();
	}

	@AttrName("LicenseMode")
	public String getLicenseMode() {
		return super.getMXAttr();
	}

	@AttrName("MemberIds")
	public int[] getMemberIds() {
		return super.getMXAttr();
	}

	@AttrName("Members")
	public String[] getMembers() {
		return super.getMXAttr();
	}

	@AttrName("MembersDeparted")
	public String[] getMembersDeparted() {
		return super.getMXAttr();
	}

	@AttrName("MembersDepartureCount")
	public long getMembersDepartureCount() {
		return super.getMXAttr();
	}

	@AttrName("OldestMemberId")
	public int getOldestMemberId() {
		return super.getMXAttr();
	}

	@AttrName("Version")
	public String getVersion() {
		return super.getMXAttr();
	}
}

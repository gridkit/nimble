package org.gridkit.lab.util.jmx.mxstruct.coherence;

import javax.management.ObjectName;

import org.gridkit.lab.util.jmx.mxstruct.MXStruct;

public class ConnectionManagerMXStruct extends MXStruct {

	public static ObjectName NAME = name("Coherence:type=ConnectionManager,name=*,nodeId=*");
	public static ConnectionManagerMXStruct PROTO = new ConnectionManagerMXStruct();

	@AttrName("ConnectionCount")
	public int getConnectionCount() {
		return super.getMXAttr();
	}

	@AttrName("HostIP")
	public String getHostIP() {
		return super.getMXAttr();
	}

	@AttrName("IncomingBufferPoolCapacity")
	public long getIncomingBufferPoolCapacity() {
		return super.getMXAttr();
	}

	@AttrName("IncomingBufferPoolSize")
	public int getIncomingBufferPoolSize() {
		return super.getMXAttr();
	}

	@AttrName("OutgoingBufferPoolCapacity")
	public long getOutgoingBufferPoolCapacity() {
		return super.getMXAttr();
	}

	@AttrName("OutgoingBufferPoolSize")
	public int getOutgoingBufferPoolSize() {
		return super.getMXAttr();
	}

	@AttrName("OutgoingByteBacklog")
	public long getOutgoingByteBacklog() {
		return super.getMXAttr();
	}

	@AttrName("OutgoingMessageBacklog")
	public long getOutgoingMessageBacklog() {
		return super.getMXAttr();
	}

	@AttrName("TotalBytesReceived")
	public long getTotalBytesReceived() {
		return super.getMXAttr();
	}

	@AttrName("TotalBytesSent")
	public long getTotalBytesSent() {
		return super.getMXAttr();
	}

	@AttrName("TotalMessagesReceived")
	public long getTotalMessagesReceived() {
		return super.getMXAttr();
	}

	@AttrName("TotalMessagesSent")
	public long getTotalMessagesSent() {
		return super.getMXAttr();
	}
}

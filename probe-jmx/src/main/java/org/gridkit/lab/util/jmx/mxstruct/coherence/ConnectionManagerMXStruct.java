package org.gridkit.lab.util.jmx.mxstruct.coherence;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.gridkit.lab.util.jmx.mxstruct.MXStruct;

public class ConnectionManagerMXStruct extends MXStruct {

	public static ObjectName NAME = name("Coherence:type=ConnectionManager,name=*,nodeId=*");
	public static ConnectionManagerMXStruct PROTO = new ConnectionManagerMXStruct();

	private String serviceName;
	
	@Override
	@SuppressWarnings("unchecked")
	public <V extends MXStruct> V read(MBeanServerConnection conn,	ObjectName name) throws ReflectionException, IOException {
		ConnectionManagerMXStruct result = super.read(conn, name);
		result.serviceName = name.getKeyProperty("name"); 
		return (V)result;
	}

	public String getServiceName() {
		return serviceName;
	}
	
	@AttrName("ConnectionCount")
	public int getConnectionCount() {
		return (Integer)super.getMXAttr();
	}

	@AttrName("HostIP")
	public String getHostIP() {
		return super.getMXAttr();
	}

	@AttrName("IncomingBufferPoolCapacity")
	public long getIncomingBufferPoolCapacity() {
		return (Long)super.getMXAttr();
	}

	@AttrName("IncomingBufferPoolSize")
	public int getIncomingBufferPoolSize() {
		return (Integer)super.getMXAttr();
	}

	@AttrName("OutgoingBufferPoolCapacity")
	public long getOutgoingBufferPoolCapacity() {
		return (Long)super.getMXAttr();
	}

	@AttrName("OutgoingBufferPoolSize")
	public int getOutgoingBufferPoolSize() {
		return (Integer)super.getMXAttr();
	}

	@AttrName("OutgoingByteBacklog")
	public long getOutgoingByteBacklog() {
		return (Long)super.getMXAttr();
	}

	@AttrName("OutgoingMessageBacklog")
	public long getOutgoingMessageBacklog() {
		return (Long)super.getMXAttr();
	}

	@AttrName("TotalBytesReceived")
	public long getTotalBytesReceived() {
		return (Long)super.getMXAttr();
	}

	@AttrName("TotalBytesSent")
	public long getTotalBytesSent() {
		return (Long)super.getMXAttr();
	}

	@AttrName("TotalMessagesReceived")
	public long getTotalMessagesReceived() {
		return (Long)super.getMXAttr();
	}

	@AttrName("TotalMessagesSent")
	public long getTotalMessagesSent() {
		return (Long)super.getMXAttr();
	}
}

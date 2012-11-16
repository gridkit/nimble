package org.gridkit.lab.util.jmx.mxstruct.coherence;

import java.io.IOException;
import java.util.Date;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.gridkit.lab.util.jmx.mxstruct.MXStruct;

public class ClusterNodeMXStruct extends MXStruct {
	
	public static final ObjectName NAME = name("Coherence:type=Node,nodeId=*");

	public static final ClusterNodeMXStruct PROTO = new ClusterNodeMXStruct();
	
	public static ClusterNodeMXStruct getLocal(MBeanServerConnection conn) throws ReflectionException, IOException {
		int memberId = ClusterMXStruct.get(conn).getLocalMemberId();
		ObjectName name = name(String.format("Coherence:type=Node,nodeId=%s", memberId));
		return PROTO.read(conn, name);
	}

	public static ClusterNodeMXStruct get(MBeanServerConnection conn, int memberId) throws ReflectionException, IOException {
		ObjectName name = name(String.format("Coherence:type=Node,nodeId=%s", memberId));
		return PROTO.read(conn, name);
	}
	
	@AttrName("BufferPublishSize")
	public int getBufferPublishSize() {
		return getInt();
	}
	
	@AttrName("BufferReceiveSize")
	public int getBufferReceiveSize() { 
		return getInt(); 
	} 
	
	@AttrName("CpuCount")
	public int getCpuCount() { 
		return getInt(); 
	} 
	
	@AttrName("Id")
	public int getId() { 
		return getInt(); 
	} 
	
	@AttrName("LoggingDestination")
	public String getLoggingDestination() { 
		return getMXAttr(); 
	}
	
	@AttrName("LoggingFormat")
	public String getLoggingFormat() { 
		return getMXAttr(); 
	} 
	
	@AttrName("LoggingLevel")
	public int getLoggingLevel() { 
		return getInt(); 
	} 
	
	@AttrName("LoggingLimit")
	public int getLoggingLimit() { 
		return getInt(); 
	} 
	
	@AttrName("MachineId")
	public int getMachineId() { 
		return getInt(); 
	} 
	
	@AttrName("MachineName")
	public String getMachineName() { 
		return getMXAttr(); 
	} 
	
	@AttrName("MemberName")
	public String getMemberName() { 
		return getMXAttr(); 
	} 
	
	@AttrName("MemoryAvailableMB")
	public int getMemoryAvailableMB() { 
		return getInt(); 
	} 
	
	@AttrName("MemoryMaxMB")
	public int getMemoryMaxMB() { 
		return getInt(); 
	} 
	
	@AttrName("MulticastAddress")
	public String getMulticastAddress() { 
		return getMXAttr(); 
	} 
	
	@AttrName("MulticastPort")
	public int getMulticastPort() { 
		return getInt(); 
	} 
	
	@AttrName("MulticastTTL")
	public int getMulticastTTL() { 
		return getInt(); 
	} 
	
	@AttrName("MulticastThreshold")
	public int getMulticastThreshold() { 
		return getInt(); 
	} 
	
	@AttrName("NackSent")
	public long getNackSent() { 
		return getLong(); 
	}
	
	@AttrName("PacketDeliveryEfficiency")
	public float getPacketDeliveryEfficiency() { 
		return getFloat(); 
	} 
	
	@AttrName("PacketsBundled")
	public long getPacketsBundled() { 
		return getLong(); 
	} 
	
	@AttrName("PacketsReceived")
	public long getPacketsReceived() { 
		return getLong(); 
	} 
	
	@AttrName("PacketsRepeated")
	public long getPacketsRepeated() { 
		return getLong(); 
	} 
	
	@AttrName("PacketsResent")
	public long getPacketsResent() { 
		return getLong(); 
	} 
	
	@AttrName("PacketsResentEarly")
	public long getPacketsResentEarly() { 
		return getLong(); 
	}
	
	@AttrName("PacketsResentExcess")
	public long getPacketsResentExcess() { 
		return getLong(); 
	}
	
	@AttrName("PacketsSent")
	public long getPacketsSent() { 
		return getLong(); 
	}
	
	@AttrName("Priority")
	public int getPriority() { 
		return getInt(); 
	} 
	
	@AttrName("ProcessName")
	public String getProcessName() { 
		return getMXAttr(); 
	}
	
	@AttrName("ProductEditio")
	public String getProductEdition() { 
		return getMXAttr(); 
	} 
	
	@AttrName("PublisherPacketUtilization")
	public float getPublisherPacketUtilization() { 
		return getFloat(); 
	} 
	
	@AttrName("PublisherSuccessRate")
	public float getPublisherSuccessRate() { 
		return getFloat(); 
	} 
	
	@AttrName("QuorumStatus")
	public String getQuorumStatus() { 
		return getMXAttr(); 
	} 
	
	@AttrName("RackName")
	public String getRackName() { 
		return getMXAttr(); 
	} 
	
	@AttrName("ReceiverPacketUtilization")
	public float getReceiverPacketUtilization() { 
		return getFloat(); 
	}
	
	@AttrName("ReceiverSuccessRate")
	public float getReceiverSuccessRate() { 
		return getFloat(); 
	} 
	
	@AttrName("ResendDelay")
	public int getResendDelay() { 
		return getInt(); 
	} 
	
	@AttrName("RoleName")
	public String getRoleName() { 
		return getMXAttr(); 
	} 
	
	@AttrName("SendAckDelay")
	public int getSendAckDelay() { 
		return getInt(); 
	} 
	
	@AttrName("SendQueueSize")
	public int getSendQueueSize() { 
		return getInt(); 
	} 
	
	@AttrName("SiteName")
	public String getSiteName() { 
		return getMXAttr(); 
	} 
	
	@AttrName("SocketCount")
	public int getSocketCount() { 
		return getInt(); 
	} 
	
	@AttrName("Statistics")
	public String getStatistics() { 
		return getMXAttr(); 
	}
	
	@AttrName("TcpRingFailures")
	public long getTcpRingFailures() { 
		return getLong(); 
	} 
	
	@AttrName("Timestamp")
	public Date getTimestamp() { 
		return getMXAttr(); 
	} 
	
	@AttrName("TrafficJamCount")
	public int getTrafficJamCount() { 
		return getInt(); 
	} 
	
	@AttrName("TrafficJamDelay")
	public int getTrafficJamDelay() { 
		return getInt(); 
	} 
	
	@AttrName("UnicastAddress")
	public String getUnicastAddress() { 
		return getMXAttr(); 
	} 
	
	@AttrName("UnicastPort")
	public int getUnicastPort() { 
		return getInt(); 
	} 
	
	@AttrName("WeakestChannel")
	public int getWeakestChannel() { 
		return getInt(); 
	}
	
	@AttrName("WellKnownAddresses")
	public String[] getWellKnownAddresses() { 
		return getMXAttr(); 
	} 
}

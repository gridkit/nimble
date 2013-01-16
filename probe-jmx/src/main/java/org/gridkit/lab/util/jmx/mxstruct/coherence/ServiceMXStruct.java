package org.gridkit.lab.util.jmx.mxstruct.coherence;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.gridkit.lab.util.jmx.mxstruct.MXStruct;

public class ServiceMXStruct extends MXStruct {

	public static ObjectName NAME = name("Coherence:type=Service,name=*,nodeId=*");
	public static ServiceMXStruct PROTO = new ServiceMXStruct();
	
	private String serviceName;	

	@Override
	@SuppressWarnings("unchecked")
	public <V extends MXStruct> V read(MBeanServerConnection conn,	ObjectName name) throws ReflectionException, IOException {
		ServiceMXStruct result = super.read(conn, name);
		result.serviceName = name.getKeyProperty("name"); 
		return (V)result;
	}

	public String getServiceName() {
		return serviceName;
	}	
	
	@AttrName("BackupCount")
	public int getBackupCount() {
		return super.getInt();
	}

	@AttrName("BackupCountAfterWritebehind")
	public int getBackupCountAfterWritebehind() {
		return super.getInt();
	}

	@AttrName("OwnedPartitionsBackup")
	public int getOwnedPartitionsBackup() {
		return super.getInt();
	}

	@AttrName("OwnedPartitionsPrimary")
	public int getOwnedPartitionsPrimary() {
		return super.getInt();
	}

	@AttrName("PartitionsAll")
	public int getPartitionsAll() {
		return super.getInt();
	}

	@AttrName("PartitionsEndangered")
	public int getPartitionsEndangered() {
		return super.getInt();
	}

	@AttrName("PartitionsUnbalanced")
	public int getPartitionsUnbalanced() {
		return super.getInt();
	}

	@AttrName("PartitionsVulnerable")
	public int getPartitionsVulnerable() {
		return super.getInt();
	}

	@AttrName("QuorumStatus")
	public String getQuorumStatus() {
		return super.getMXAttr();
	}

	@AttrName("RequestAverageDuration")
	public float getRequestAverageDuration() {
		return super.getFloat();
	}

	@AttrName("RequestMaxDuration")
	public long getRequestMaxDuration() {
		return super.getLong();
	}

	@AttrName("RequestPendingCount")
	public long getRequestPendingCount() {
		return super.getLong();
	}

	@AttrName("RequestPendingDuration")
	public long getRequestPendingDuration() {
		return super.getLong();
	}

	@AttrName("RequestTimeoutCount")
	public long getRequestTimeoutCount() {
		return super.getLong();
	}

	@AttrName("RequestTimeoutMillis")
	public long getRequestTimeoutMillis() {
		return super.getLong();
	}

	@AttrName("RequestTotalCount")
	public long getRequestTotalCount() {
		return super.getLong();
	}

	@AttrName("SeniorMemberId")
	public int getSeniorMemberId() {
		return super.getInt();
	}

	@AttrName("Statistics")
	public String getStatistics() {
		return super.getMXAttr();
	}

	@AttrName("StatusHA")
	public String getStatusHA() {
		return super.getMXAttr();
	}

	@AttrName("StorageEnabledCount")
	public int getStorageEnabledCount() {
		return super.getInt();
	}

	@AttrName("TaskAverageDuration")
	public float getTaskAverageDuration() {
		return super.getFloat();
	}

	@AttrName("TaskBacklog")
	public int getTaskBacklog() {
		return super.getInt();
	}

	@AttrName("TaskCount")
	public long getTaskCount() {
		return super.getLong();
	}

	@AttrName("TaskHungCount")
	public int getTaskHungCount() {
		return super.getInt();
	}

	@AttrName("TaskHungDuration")
	public long getTaskHungDuration() {
		return super.getLong();
	}

	@AttrName("TaskHungTaskId")
	public String getTaskHungTaskId() {
		return super.getMXAttr();
	}

	@AttrName("TaskHungThresholdMillis")
	public long getTaskHungThresholdMillis() {
		return super.getLong();
	}

	@AttrName("TaskMaxBacklog")
	public int getTaskMaxBacklog() {
		return super.getInt();
	}

	@AttrName("TaskTimeoutCount")
	public int getTaskTimeoutCount() {
		return super.getInt();
	}

	@AttrName("TaskTimeoutMillis")
	public long getTaskTimeoutMillis() {
		return super.getLong();
	}

	@AttrName("ThreadAbandonedCount")
	public int getThreadAbandonedCount() {
		return super.getInt();
	}

	@AttrName("ThreadAverageActiveCount")
	public float getThreadAverageActiveCount() {
		return super.getFloat();
	}

	@AttrName("ThreadCount")
	public int getThreadCount() {
		return super.getInt();
	}

	@AttrName("ThreadIdleCount")
	public int getThreadIdleCount() {
		return super.getInt();
	}

	@AttrName("Type")
	public String getType() {
		return super.getMXAttr();
	}
}

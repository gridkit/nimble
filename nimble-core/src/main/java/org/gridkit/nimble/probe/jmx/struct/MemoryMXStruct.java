package org.gridkit.nimble.probe.jmx.struct;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.gridkit.nimble.probe.jmx.MXStruct;

public class MemoryMXStruct extends MXStruct{

	public static final ObjectName NAME = name("java.lang:type=Memory");
	public static final MemoryMXStruct PROTO = new MemoryMXStruct();
	
	public static MemoryMXStruct get(MBeanServerConnection conn) throws ReflectionException, IOException {
		return PROTO.read(conn, NAME);
	}
	
	@AttrName("HeapMemoryUsage") @AsObject(MemUsage.class)
	public MemUsage getHeapMemoryUsage() {
		return getMXAttr();
	}
	
	@AttrName("NonHeapMemoryUsage") @AsObject(MemUsage.class)
	public MemUsage getNonHeapMemoryUsage() {
		return getMXAttr();
	}
	
	@AttrName("ObjectPendingFinalizationCount")
	public int getObjectPendingFinalizationCount() {
		return (Integer)getMXAttr();
	}

	@AttrName("Verbose")
	public boolean isVerbose() {
		return (Boolean)getMXAttr();
	}
	
	public static class MemUsage extends MXStruct {

		@AttrName("init")
		public long getInit() {
			return (Long)getMXAttr();
		}

		@AttrName("used")
		public long getUsed() {
			return (Long)getMXAttr();
		}

		@AttrName("committed")
		public long getCommitted() {
			return (Long)getMXAttr();
		}

		@AttrName("max")
		public long getMax() {
			return (Long)getMXAttr();
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(toMemorySize(getInit()));
			sb.append("/").append(toMemorySize(getUsed()));
			sb.append("/").append(toMemorySize(getCommitted()));
			if (getMax() > 0) {
				sb.append("/").append(toMemorySize(getMax()));
			}
			else {
				sb.append("/NA");
			}
			return sb.toString();
		}
	}
	
	static final String toMemorySize(long n) {
		if (n < (10l << 10)) {
			return String.valueOf(n);
		}
		else if (n < (10l << 20)) {
			return String.valueOf(n >> 10) + "k";
		}
		else if (n < (10l << 30)) {
			return String.valueOf(n >> 20) + "m";
		}
		else {
			return String.valueOf(n >> 30) + "g";
		}
	}
}

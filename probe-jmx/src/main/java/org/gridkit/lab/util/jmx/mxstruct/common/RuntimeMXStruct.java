package org.gridkit.lab.util.jmx.mxstruct.common;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.gridkit.lab.util.jmx.mxstruct.MXStruct;
import org.gridkit.lab.util.jmx.mxstruct.MXHelper;

public class RuntimeMXStruct extends MXStruct implements RuntimeMXBean {
	
	public static ObjectName NAME = name(ManagementFactory.RUNTIME_MXBEAN_NAME);
	public static RuntimeMXStruct PROTO = new RuntimeMXStruct();
	
	public static RuntimeMXStruct get(MBeanServerConnection conn) {
		try {
			return MXHelper.collectBeans(conn, NAME, PROTO).values().iterator().next();
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}
	
	@Override
	@AttrName("Name")
	public String getName() {
		return super.getMXAttr();
	}
	
	@Override
	@AttrName("VmName")
	public String getVmName() {
		return super.getMXAttr();
	}
	
	@Override
	@AttrName("VmVendor")
	public String getVmVendor() {
		return super.getMXAttr();
	}
	
	@Override
	@AttrName("VmVersion")
	public String getVmVersion() {
		return super.getMXAttr();
	}
	
	@Override
	@AttrName("SpecName")
	public String getSpecName() {
		return super.getMXAttr();
	}
	
	@Override
	@AttrName("SpecVendor")
	public String getSpecVendor() {
		return super.getMXAttr();
	}
	
	@Override
	@AttrName("SpecVersion")
	public String getSpecVersion() {
		return super.getMXAttr();
	}
	
	@Override
	@AttrName("ManagementSpecVersion")
	public String getManagementSpecVersion() {
		return super.getMXAttr();
	}
	
	@Override
	@AttrName("ClassPath")
	public String getClassPath() {
		return super.getMXAttr();
	}
	
	@Override
	@AttrName("LibraryPath")
	public String getLibraryPath() {
		return super.getMXAttr();
	}
	
	@Override
	@AttrName("BootClassPathSupported")
	public boolean isBootClassPathSupported() {
		return (Boolean)super.getMXAttr();
	}
	
	@Override
	@AttrName("BootClassPath")
	public String getBootClassPath() {
		return super.getMXAttr();
	}
	
	@Override
	@AttrName("InputArguments") @AsCollection
	public List<String> getInputArguments() {
		return super.getMXAttr();
	}
	
	@Override
	@AttrName("Uptime")
	public long getUptime() {
		return (Long)super.getMXAttr();
	}
	
	@Override
	@AttrName("StartTime")
	public long getStartTime() {
		return (Long)super.getMXAttr();
	}
	
	@Override
	@AttrName("SystemProperties") @AsMap(key="key", val="value")
	public Map<String, String> getSystemProperties() {
		return super.getMXAttr();
	}

	// TODO artem.panasyuk: correct migration to 1.7
    public ObjectName getObjectName() {
        return null;
    }
}

package org.gridkit.nimble.probe.jmx.mxstruct;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Map;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.gridkit.lab.util.jmx.mxstruct.MXHelper;
import org.gridkit.lab.util.jmx.mxstruct.common.GarbageCollectorMXStruct;
import org.gridkit.lab.util.jmx.mxstruct.common.GarbageCollectorMXStruct.LastGcInfo;
import org.gridkit.lab.util.jmx.mxstruct.common.MemoryMXStruct;
import org.gridkit.lab.util.jmx.mxstruct.common.MemoryMXStruct.MemUsage;
import org.gridkit.lab.util.jmx.mxstruct.common.MemoryPoolMXStruct;
import org.gridkit.lab.util.jmx.mxstruct.common.RuntimeMXStruct;
import org.junit.Assert;
import org.junit.Test;


public class MXStructTest {

	private MBeanServerConnection conn = ManagementFactory.getPlatformMBeanServer();
	
	@Test
	public void test_all_attrs() throws ReflectionException, IOException {
		MemoryMXStruct membean = MemoryMXStruct.PROTO.read(conn, MemoryMXStruct.NAME);
		
		membean.getHeapMemoryUsage().getInit();
		membean.getHeapMemoryUsage().getUsed();
		membean.getHeapMemoryUsage().getCommitted();
		membean.getHeapMemoryUsage().getMax();

		membean.getNonHeapMemoryUsage().getInit();
		membean.getNonHeapMemoryUsage().getUsed();
		membean.getNonHeapMemoryUsage().getCommitted();
		membean.getNonHeapMemoryUsage().getMax();
		
		membean.getHeapMemoryUsage().toString();
		
		membean.getObjectPendingFinalizationCount();
		membean.isVerbose();
	}
	
	@Test
	public void test_gc_beans() throws MalformedObjectNameException, NullPointerException, IOException, ReflectionException {		
		Map<String, GarbageCollectorMXStruct> beans = MXHelper.collectBeans(conn, GarbageCollectorMXStruct.PATTERN, GarbageCollectorMXStruct.PROTO);
		Assert.assertTrue(beans.size() > 0);
		for(GarbageCollectorMXStruct bean: beans.values()) {
			bean.getName();
			bean.getCollectionCount();
			bean.getCollectionTime();
			bean.getMemoryPoolNames();
			bean.isValid();
			
			LastGcInfo lastGcInfo = bean.getLastGcInfo();
			lastGcInfo.getId();
			lastGcInfo.getStartTime();
			lastGcInfo.getEndTime();
			lastGcInfo.getDuration();
			
			Map<String, MemUsage> bgc = lastGcInfo.getMemoryUsageBeforeGc();
			Assert.assertTrue(bgc.size() > 0);
			for(MemUsage mu: bgc.values()) {
				mu.getInit();
				mu.getUsed();
				mu.getCommitted();
				mu.getMax();
			}

			Map<String, MemUsage> agc = lastGcInfo.getMemoryUsageAfterGc();
			Assert.assertTrue(agc.size() > 0);
			for(MemUsage mu: agc.values()) {
				mu.getInit();
				mu.getUsed();
				mu.getCommitted();
				mu.getMax();
			}
		}
	}

	@Test
	public void test_memory_pool_beans() throws MalformedObjectNameException, NullPointerException, IOException, ReflectionException {		
		Map<String, MemoryPoolMXStruct> beans = MXHelper.collectBeans(conn, MemoryPoolMXStruct.PATTERN, MemoryPoolMXStruct.PROTO);
		Assert.assertTrue(beans.size() > 0);
		for(MemoryPoolMXStruct bean: beans.values()) {
			bean.getName();
			bean.getType();
			bean.getMemoryManagerNames();
			bean.getUsage().getInit();
			bean.getUsage().getUsed();
			bean.getUsage().getCommitted();
			bean.getUsage().getMax();
			bean.getPeakUsage().getInit();
			bean.getPeakUsage().getUsed();
			bean.getPeakUsage().getCommitted();
			bean.getPeakUsage().getMax();
			bean.isValid();
		}
	}	
	
	@Test
	public void test_runtime_mbean() throws ReflectionException, IOException {
		
		System.setProperty("test-prop", "test-value");
		
		MBeanServerConnection conn = ManagementFactory.getPlatformMBeanServer();
		RuntimeMXStruct rt = RuntimeMXStruct.get(conn);
		
		Assert.assertEquals(System.getProperty("java.runtime.version"), rt.getSystemProperties().get("java.runtime.version"));
		Assert.assertEquals("test-value", rt.getSystemProperties().get("test-prop"));
		
		RuntimeMXBean rrt = ManagementFactory.getRuntimeMXBean();
		Assert.assertEquals(rrt.getBootClassPath(), 	rt.getBootClassPath());
		Assert.assertEquals(rrt.getClassPath(), 		rt.getClassPath());
		Assert.assertEquals(rrt.getLibraryPath(), 		rt.getLibraryPath());
		Assert.assertEquals(rrt.getManagementSpecVersion(), rt.getManagementSpecVersion());
		Assert.assertEquals(rrt.getName(), 				rt.getName());
		Assert.assertEquals(rrt.getSpecName(), 			rt.getSpecName());
		Assert.assertEquals(rrt.getSpecVendor(), 		rt.getSpecVendor());
		Assert.assertEquals(rrt.getSpecVersion(), 		rt.getSpecVersion());
		Assert.assertEquals(rrt.getStartTime(), 		rt.getStartTime());
//		Assert.assertEquals(rrt.getUptime(), 			rt.getUptime());
		Assert.assertEquals(rrt.getVmName(), 			rt.getVmName());
		Assert.assertEquals(rrt.getVmVendor(), 			rt.getVmVendor());
		Assert.assertEquals(rrt.getVmVersion(), 		rt.getVmVersion());
		Assert.assertEquals(rrt.getInputArguments(), 	rt.getInputArguments());
		Assert.assertEquals(rrt.getSystemProperties(),	rt.getSystemProperties());
	}
	
	@Test
	public void test_provided_runtime_mbean() throws ReflectionException, IOException, MalformedObjectNameException, NullPointerException {
		
		System.setProperty("test-prop", "test-value");
		
		MBeanServerConnection conn = ManagementFactory.getPlatformMBeanServer();
		RuntimeMXBean rt = JMX.newMXBeanProxy(conn, new ObjectName(ManagementFactory.RUNTIME_MXBEAN_NAME), RuntimeMXBean.class);
		
		Assert.assertEquals(System.getProperty("java.runtime.version"), rt.getSystemProperties().get("java.runtime.version"));
		Assert.assertEquals("test-value", rt.getSystemProperties().get("test-prop"));
		
		RuntimeMXBean rrt = ManagementFactory.getRuntimeMXBean();
		Assert.assertEquals(rrt.getBootClassPath(), 	rt.getBootClassPath());
		Assert.assertEquals(rrt.getClassPath(), 		rt.getClassPath());
		Assert.assertEquals(rrt.getLibraryPath(), 		rt.getLibraryPath());
		Assert.assertEquals(rrt.getManagementSpecVersion(), rt.getManagementSpecVersion());
		Assert.assertEquals(rrt.getName(), 				rt.getName());
		Assert.assertEquals(rrt.getSpecName(), 			rt.getSpecName());
		Assert.assertEquals(rrt.getSpecVendor(), 		rt.getSpecVendor());
		Assert.assertEquals(rrt.getSpecVersion(), 		rt.getSpecVersion());
		Assert.assertEquals(rrt.getStartTime(), 		rt.getStartTime());
//		Assert.assertEquals(rrt.getUptime(), 			rt.getUptime());
		Assert.assertEquals(rrt.getVmName(), 			rt.getVmName());
		Assert.assertEquals(rrt.getVmVendor(), 			rt.getVmVendor());
		Assert.assertEquals(rrt.getVmVersion(), 		rt.getVmVersion());
		Assert.assertEquals(rrt.getInputArguments(), 	rt.getInputArguments());
		Assert.assertEquals(rrt.getSystemProperties(),	rt.getSystemProperties());
	}	
}

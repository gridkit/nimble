package org.gridkit.nimble.sensor.jmx;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.gridkit.lab.jvm.attach.PatternJvmMatcher;
import org.gridkit.nimble.probe.JvmMatcherPidProvider;
import org.junit.Test;

public class MBeanProbeTest {

	@Test
	public void test() throws MalformedObjectNameException, NullPointerException, InterruptedException {
		
		PatternJvmMatcher matcher = new PatternJvmMatcher();
		JvmMatcherPidProvider pp = new JvmMatcherPidProvider(matcher);
		
		
		MBeanProbe probe1 = new MBeanProbe(pp, new ObjectName("java.lang:type=OperatingSystem"), "CommittedVirtualMemorySize");
		MBeanProbe probe2 = new MBeanProbe(pp, new ObjectName("java.lang:type=OperatingSystem"), "FreePhysicalMemorySize");
		MBeanProbe probe3 = new MBeanProbe(pp, new ObjectName("java.lang:type=OperatingSystem"), "FreeSwapSpaceSize");
		
		for(int i = 0; i != 20; ++i) {
			System.out.println(probe1.measure());
			System.out.println(probe2.measure());
			System.out.println(probe3.measure());
		}		
	}
}

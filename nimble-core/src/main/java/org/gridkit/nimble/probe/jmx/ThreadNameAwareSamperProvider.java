package org.gridkit.nimble.probe.jmx;

public interface ThreadNameAwareSamperProvider {
	
	public JavaThreadStatsSampler getSampler(String threadName); 

}

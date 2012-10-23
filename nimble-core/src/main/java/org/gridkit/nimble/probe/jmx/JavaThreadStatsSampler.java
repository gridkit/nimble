package org.gridkit.nimble.probe.jmx;

public interface JavaThreadStatsSampler {

	public void report(long startNanos, long finishNanos, long threadId, String threadName, long cpuTime, long userTime, long blockedTime, long blockedCount, long waitTime, long waitCount, long allocated);
	
}

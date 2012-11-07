package org.gridkit.nimble.probe.jmx.threading;

public interface JavaThreadStatsSampler {

	/**
	 * @param startNanos - start of reported interval (local nanotime)
	 * @param finishNanos - end of reported interval (local nanotime)
	 * @param threadId
	 * @param threadName
	 * @param cpuTime - total CPU time in nanos
	 * @param userTime - user CPU time in nanos
	 * @param blockedTime - blocked time in nanos (if available)
	 * @param blockedCount
	 * @param waitTime - wait time in nanos (if available)
	 * @param waitCount
	 * @param allocated
	 */
	public void report(long startNanos, long finishNanos, long threadId, String threadName, long cpuTime, long userTime, long blockedTime, long blockedCount, long waitTime, long waitCount, long allocated);
	
}

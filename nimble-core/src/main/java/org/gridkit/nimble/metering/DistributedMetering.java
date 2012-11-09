package org.gridkit.nimble.metering;

public enum DistributedMetering implements SampleKey {

	/** 
	 * Logical name of node/process there sample was collected. 
	 */
	NODENAME,
	
	/** 
	 * ID of process related to a sample.
	 * 
	 * {@link #PID} and {@link #HOSTNAME} should uniquely identify process in cluster. 
	 */ 
	PID,
	
	/** 
	 * Name of host there sample was collected.
	 * 
	 * {@link #PID} and {@link #HOSTNAME} should uniquely identify process in cluster.
	 */
	HOSTNAME,
	
}

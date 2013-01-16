package org.gridkit.nimble.monitoring.coherence;

import org.gridkit.nimble.monitoring.MonitoringBundle;


public interface CoherenceMonitoringBundle extends MonitoringBundle {

	public static final Object CLUSTER = CoherenceMetricsKey.CLUSTER; 
	public static final Object MEMBER_ROLE = CoherenceMetricsKey.MEMBER_ROLE; 
	public static final Object SERVICE_NAME = CoherenceMetricsKey.SERVICE_NAME; 
	public static final Object THREAD_TYPE = CoherenceMetricsKey.THREAD_TYPE;;
}

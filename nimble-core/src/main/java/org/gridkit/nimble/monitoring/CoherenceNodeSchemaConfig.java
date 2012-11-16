package org.gridkit.nimble.monitoring;

import java.io.Serializable;

import javax.management.MBeanServerConnection;

import org.gridkit.lab.util.jmx.mxstruct.coherence.ClusterMXStruct;
import org.gridkit.lab.util.jmx.mxstruct.coherence.ClusterNodeMXStruct;
import org.gridkit.lab.util.jmx.mxstruct.common.RuntimeMXStruct;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.probe.probe.SchemaConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoherenceNodeSchemaConfig implements SchemaConfigurer<MBeanServerConnection>, Serializable {

	private static final long serialVersionUID = 20121114L;

	private static final Logger LOGGER = LoggerFactory.getLogger(CoherenceNodeSchemaConfig.class);
	
	private final SchemaConfigurer<MBeanServerConnection> parentConfig;

	public CoherenceNodeSchemaConfig(SchemaConfigurer<MBeanServerConnection> parentConfig) {
		this.parentConfig = parentConfig;
	}

	@Override
	public SampleSchema configure(MBeanServerConnection target, SampleSchema root) {
		SampleSchema schema = parentConfig == null ? root.createDerivedScheme() : parentConfig.configure(target, root).createDerivedScheme();
		if (schema == null) {
			return null;
		}
		ClusterMXStruct cluster;
		try {
			cluster = ClusterMXStruct.get(target);
			if (cluster != null) {
				schema.setStatic(CoherenceMonitoringBundle.CLUSTER, cluster.getClusterName());
				ClusterNodeMXStruct node = ClusterNodeMXStruct.getLocal(target);
				schema.setStatic(CoherenceMonitoringBundle.MEMBER_ROLE, node.getRoleName());
				
				LOGGER.info("CoherenceNode: cluster=" + cluster.getClusterName() + ",id=" + node.getId() + ", role=" + node.getRoleName());
				
				schema.freeze();
				return schema;
			}
			else {
				RuntimeMXStruct runtime = RuntimeMXStruct.get(target);
				LOGGER.warn("No Coherence cluster MBean found: " + runtime.getName());
				return null;
			}
		} catch (Exception e) {
			
			LOGGER.warn("Failed to read cluster details from process ", e);
			return null;
		}
	}
}

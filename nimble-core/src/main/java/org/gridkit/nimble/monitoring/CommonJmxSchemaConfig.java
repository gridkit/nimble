package org.gridkit.nimble.monitoring;

import java.io.Serializable;

import javax.management.MBeanServerConnection;

import org.gridkit.lab.util.jmx.mxstruct.common.RuntimeMXStruct;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.probe.probe.JmxProbes;
import org.gridkit.nimble.probe.probe.SchemaConfigurer;

public class CommonJmxSchemaConfig implements SchemaConfigurer<MBeanServerConnection>, Serializable {
	
	private static final long serialVersionUID = 20121114L;

	public static final Object JVM_ID = JmxProbes.JVM_ID;
	
	private final SchemaConfigurer<MBeanServerConnection> parent;

	public CommonJmxSchemaConfig() {
		this(null);
	}
	
	public CommonJmxSchemaConfig(SchemaConfigurer<MBeanServerConnection> parent) {
		this.parent = parent;
	}

	@Override
	public SampleSchema configure(MBeanServerConnection target, SampleSchema root) {
		SampleSchema schema = parent != null ? parent.configure(target, root) : root.createDerivedScheme();
		schema.setStatic(JVM_ID, RuntimeMXStruct.get(target).getName());
		schema.freeze();
		return schema;
	}
}

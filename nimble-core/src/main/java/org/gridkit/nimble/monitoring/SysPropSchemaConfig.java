package org.gridkit.nimble.monitoring;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;

import org.gridkit.lab.jvm.attach.AttachManager;
import org.gridkit.lab.util.jmx.mxstruct.common.RuntimeMXStruct;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.probe.probe.SchemaConfigurer;

public class SysPropSchemaConfig implements Serializable {
	
	private static final long serialVersionUID = 20121114L;
	private final Map<Object, Extractor> extractors = new HashMap<Object, Extractor>();
	
	
	protected SysPropSchemaConfig() {
	}

	public void readProp(String prop, Object attr) {
		extractors.put(attr, new PropExtractor(prop));
	}
	
	protected void configure(SampleSchema schema, Map<String, String> props) {
		for(Object key: extractors.keySet()) {
			Object v = extractors.get(key).get(props);
			if (v != null) {
				schema.setStatic(key, v);
			}
		}
	}

	public static class MBean extends SysPropSchemaConfig implements SchemaConfigurer<MBeanServerConnection>, Serializable {
		
		private static final long serialVersionUID = 20121118L;
		
		private final SchemaConfigurer<MBeanServerConnection> next;

		public MBean() {
			this(null);
		}

		public MBean(SchemaConfigurer<MBeanServerConnection> next) {
			this.next = next;
		}
		
		@Override
		public SampleSchema configure(MBeanServerConnection target, SampleSchema root) {
			SampleSchema schema = (next != null ? next.configure(target, root) : root).createDerivedScheme();
			configure(schema, RuntimeMXStruct.get(target).getSystemProperties());
			schema.freeze();
			return schema;
		}
	}

	public static class ProcessId extends SysPropSchemaConfig implements SchemaConfigurer<Long>, Serializable {
		
		private static final long serialVersionUID = 20121118L;
		
		private final SchemaConfigurer<Long> next;
		
		public ProcessId() {
			this(null);
		}
		
		public ProcessId(SchemaConfigurer<Long> next) {
			this.next = next;
		}
		
		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public SampleSchema configure(Long target, SampleSchema root) {
			SampleSchema schema = (next != null ? next.configure(target, root) : root).createDerivedScheme();			
			configure(schema, (Map)AttachManager.getDetails(target).getSystemProperties());
			schema.freeze();
			return schema;
		}
	}
	
	private interface Extractor {
		
		public Object get(Map<String, String> props); 
		
	}
	
	private static class PropExtractor implements Extractor, Serializable {
		
		private static final long serialVersionUID = 20121118L;
		
		private final String name;

		private PropExtractor(String name) {
			this.name = name;
		}

		@Override
		public Object get(Map<String, String> props) {
			return props.get(name);
		}
	}
}

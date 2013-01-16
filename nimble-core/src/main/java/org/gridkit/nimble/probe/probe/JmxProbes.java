package org.gridkit.nimble.probe.probe;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.gridkit.lab.jvm.attach.AttachManager;
import org.gridkit.lab.monitoring.probe.TargetLocator;
import org.gridkit.nimble.driver.Activity;
import org.gridkit.nimble.metering.SampleKey;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.probe.jmx.JmxLocator;
import org.gridkit.nimble.probe.jmx.MBeanConnector;
import org.gridkit.nimble.probe.jmx.MBeanLocator;
import org.gridkit.nimble.probe.jmx.MBeanProbe;
import org.gridkit.nimble.probe.jmx.MBeanSampler;
import org.gridkit.nimble.probe.jmx.MBeanTarget;
import org.gridkit.nimble.probe.jmx.threading.JavaThreadStatsSampler;
import org.gridkit.nimble.probe.jmx.threading.JavaThreadingProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmxProbes {

	private static final Logger LOGGER = LoggerFactory.getLogger(JmxProbes.class);
	
	public static Object JVM_ID = JmxMetricsKey.PROCESS_ID;
	
	private enum JmxMetricsKey implements SampleKey {
		PROCESS_ID;
	}
	
	public static Activity deployJavaThreadProbe(MonitoringDriver pollDriver, MBeanConnector connector, SchemaConfigurer<MBeanServerConnection> schemaConfig, SamplerPrototype<JavaThreadStatsSampler> samplerProto) {
		return deployJavaThreadProbe(pollDriver, connector, schemaConfig, samplerProto, 1000);
	}

	public static Activity deployJavaThreadProbe(MonitoringDriver pollDriver, MBeanConnector connector, SchemaConfigurer<MBeanServerConnection> schemaConfig, SamplerPrototype<JavaThreadStatsSampler> samplerProto, long periodMs) {
		return pollDriver.deploy(new JmxLocator(connector), new JavaThreadingProbe(), schemaConfig, samplerProto, periodMs);
	}

	public static Activity deployMBeanProbe(MonitoringDriver pollDriver, MBeanConnector connector, ObjectName name, SchemaConfigurer<MBeanTarget> schemaConfig, SamplerPrototype<MBeanSampler> samplerProto) {
		return deployMBeanProbe(pollDriver, connector, name, schemaConfig, samplerProto, 1000);
	}
	
	public static Activity deployMBeanProbe(MonitoringDriver pollDriver, MBeanConnector connector, ObjectName name, SchemaConfigurer<MBeanTarget> schemaConfig, SamplerPrototype<MBeanSampler> samplerProto, long periodMs) {
		return pollDriver.deploy(new MBeanLocator(connector, name), new MBeanProbe(), schemaConfig, samplerProto, periodMs);
	}

	public static Activity deployMBeanProbe(MonitoringDriver pollDriver, TargetLocator<MBeanTarget> locator, SchemaConfigurer<MBeanTarget> schemaConfig, SamplerPrototype<MBeanSampler> samplerProto) {
		return deployMBeanProbe(pollDriver, locator, schemaConfig, samplerProto, 1000);
	}
	
	public static Activity deployMBeanProbe(MonitoringDriver pollDriver, TargetLocator<MBeanTarget> locator, SchemaConfigurer<MBeanTarget> schemaConfig, SamplerPrototype<MBeanSampler> samplerProto, long periodMs) {
		return pollDriver.deploy(locator, new MBeanProbe(), schemaConfig, samplerProto, periodMs);
	}
	
	public static SchemaConfigurer<Long> jmx2pid(SchemaConfigurer<MBeanServerConnection> config) {
		return new PidToJmxAdapter(config);
	}
	
	@SuppressWarnings("unchecked")
	public static <S> SamplerPrototype<S> combine(SamplerPrototype<?>... prototypes) {
		return new CombinedProto<S>((SamplerPrototype<S>[]) prototypes);
	}

	private static class CombinedProto<S> implements SamplerPrototype<S>, Serializable {

		private static final long serialVersionUID = 20121107L;
		
		private final SamplerPrototype<S>[] protos;
		
		public CombinedProto(SamplerPrototype<S>[] protos) {
			this.protos = protos;
		}

		@Override
		public S instantiate(SampleSchema schema) {
			List<S> samplers = new ArrayList<S>();
			for(SamplerPrototype<S> proto: protos) {
				samplers.add(proto.instantiate(schema));
			}
			S stack = newGroupProxy(samplers);
			return stack;
		}

		@SuppressWarnings("unchecked")
		private S newGroupProxy(final List<S> samplers) {
			Set<Class<?>> interfaces = null;
			for(S s: samplers) {
				if (interfaces == null) {
					interfaces = new HashSet<Class<?>>();
					interfaces.addAll(Arrays.asList(s.getClass().getInterfaces()));
				}
				else {
					interfaces.retainAll(Arrays.asList(s.getClass().getInterfaces()));
				}
			}
			interfaces.remove(Serializable.class);
			interfaces.remove(Comparable.class);
			if (interfaces.isEmpty()) {
				throw new IllegalArgumentException("Failed to create group proxy for " + samplers);
			}
			
			InvocationHandler handler = new ProxyHandler(samplers);
			
			return (S) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), interfaces.toArray(new Class<?>[0]), handler);
		}

		private class ProxyHandler implements InvocationHandler {
		
			private final List<S> samplers;
		
			private ProxyHandler(List<S> samplers) {
				this.samplers = samplers;
			}
		
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (method.getDeclaringClass() == Object.class) {
					return method.invoke(this, args);
				}
				else {
					for(S s: samplers) {
						try {
							method.invoke(s, args);
						}
						catch(Exception e) {
							// TODO logging
						}
					}
				}
				return null;
			}
		
			@Override
			public String toString() {
				return samplers.toString();
			}
		}
	}
	
	private static class PidToJmxAdapter implements SchemaConfigurer<Long>, Serializable {
		
		private static final long serialVersionUID = 20121115L;
		
		private final SchemaConfigurer<MBeanServerConnection> scheme;

		public PidToJmxAdapter(SchemaConfigurer<MBeanServerConnection> scheme) {
			this.scheme = scheme;
		}

		@Override
		public SampleSchema configure(Long target, SampleSchema root) {
			try {
				MBeanServerConnection conn = AttachManager.getDetails(target).getMBeans();
				if (conn != null) {
					return scheme.configure(conn, root);
				}
			} catch (Exception e) {
				LOGGER.warn("JMX connection failed, pid: " + target, e);
			}
			return null;
		}
	}
}

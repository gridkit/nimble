package org.gridkit.nimble.monitoring;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;

import org.gridkit.lab.util.jmx.mxstruct.common.RuntimeMXStruct;
import org.gridkit.nimble.driver.Activity;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleFactory;
import org.gridkit.nimble.metering.SampleKey;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.orchestration.ScenarioBuilder;
import org.gridkit.nimble.orchestration.TimeLine;
import org.gridkit.nimble.pivot.Pivot;
import org.gridkit.nimble.pivot.display.DisplayBuilder;
import org.gridkit.nimble.pivot.display.PrintConfig;
import org.gridkit.nimble.probe.jmx.JmxLocator;
import org.gridkit.nimble.probe.jmx.MBeanConnector;
import org.gridkit.nimble.probe.jmx.threading.JavaThreadStatsSampler;
import org.gridkit.nimble.probe.jmx.threading.JavaThreadingProbe;
import org.gridkit.nimble.probe.probe.MonitoringDriver;
import org.gridkit.nimble.probe.probe.SamplerPrototype;
import org.gridkit.nimble.probe.probe.SchemaConfigurer;
import org.gridkit.nimble.util.Seconds;

public class CoherenceExtendMonitoring extends AbstractMonitoringBundle implements CoherenceMonitoringBundle {

	public static String PACKET_SPEAKER = "PacketSpeaker";
	public static String PACKET_PUBLISHER = "PacketPublisher";
	public static String PACKET_RECEIVER = "PacketReceiver";
	public static String PACKET_LISTENER = "PacketListener";

	private enum JvmId implements SampleKey {
		ID
	}
	
	private static String[] CLUSTER_THREADS = {PACKET_SPEAKER, PACKET_PUBLISHER, PACKET_RECEIVER, PACKET_SPEAKER};
	
	private MBeanConnector connector;
	private SchemaConfigurer<MBeanServerConnection> schemaConfig = new NoSchema<MBeanServerConnection>();
	private int pollPeriod = 5000;
	
	public CoherenceExtendMonitoring(String namespace) {
		super(namespace);
	}

	@Override
	public String getDescription() {
		return "Coherence thread utilization";
	}

	public void setPollPeriod(int millis) {
		this.pollPeriod = millis;
	}
	
	public void setLocator(MBeanConnector connector) {
		this.connector = connector;
	}

	public void setSchemaConfig(SchemaConfigurer<MBeanServerConnection> schemeConfig) {
		this.schemaConfig = schemeConfig;
	}

	public void deploy(ScenarioBuilder sb, MonitoringDriver pollDriver, TimeLine phase) {
		if (connector == null) {
			throw new IllegalArgumentException("Connector is not set");
		}
		sb.from(phase.getInitCheckpoint());
		Activity probe = pollDriver.deploy(new JmxLocator(connector), new JavaThreadingProbe(), createSchemaConfig(), createSampler(), pollPeriod);
		sb.join(phase.getStartCheckpoint());
		
		sb.from(phase.getStopCheckpoint());
		probe.stop();
		sb.join(phase.getDoneCheckpoint());
		
		sb.fromStart();
		probe.join();
		sb.join(phase.getDoneCheckpoint());
	}

	@Override
	public void deploy(ScenarioBuilder sb, ServiceProvider context, TimeLine phase) {
		MonitoringDriver pollDriver = context.lookup(MonitoringDriver.class);
		deploy(sb, pollDriver, phase);
	}

	private SchemaConfigurer<MBeanServerConnection> createSchemaConfig() {
		return new SchemaEnricher(schemaConfig);
	}
	
	private SamplerPrototype<JavaThreadStatsSampler> createSampler() {
		return new SamplerProvider(getProducerId());
	}

	@Override
	public void configurePivot(Pivot pivot) {
		Pivot.Level base = pivot.root().level(namespace)
				.filter(Measure.PRODUCER, getProducerId());
		for(Object g: groupping) {
			base = base.group(g);
		}
		
		base = base.group(SERVICE_NAME);
		base = base.group(THREAD_TYPE);
				
		
		base.level("")
			.calcFrequency(Measure.MEASURE)
			.calcDistinct(JvmId.ID)
			.pivot("pid")
				.group(JvmId.ID)
					.calcFrequency(Measure.MEASURE);			
	}

	@Override
	public void configurePrinter(PrintConfig printer) {
		
		printConfig.replay(printer);
		
		DisplayBuilder.with(printer, namespace)
			.constant("Metric", "CPU usage [100% = core]")
			.attribute("Service", SERVICE_NAME)
			.attribute("Thread type",  THREAD_TYPE)
			.frequency().caption("All processes CPU [%%]").asPercent()
			.decorated("pid").calc().max().frequency().caption("Max. CPU per process [%%]").asPercent()
			.decorated("pid").calc().mean().frequency().caption("Avg. CPU per process [%%]").asPercent()
			.decorated("pid").calc().min().frequency().caption("Min. CPU per process [%%]").asPercent()
			.distinct(JvmId.ID).caption("Processes [N]")
			.duration().caption("Observed [Sec]");
	}
	
	static SampleFactory configureCoherenceThreadSampler(String threadName, SampleSchema root, Set<String> services) {
		for(String th: CLUSTER_THREADS) {
			if (threadName.startsWith(th)) {
				SampleSchema sh = root.createDerivedScheme();
				sh.setStatic(SERVICE_NAME, "Cluster");
				sh.setStatic(THREAD_TYPE, th);
				return sh.createFactory();
			}
		}
		if (threadName.startsWith("DistributedCache:")) {
			String service = serviceName(threadName);
			services.add(service);
			if (threadName.equals("DistributedCache:" + service)) {
				SampleSchema sh = root.createDerivedScheme();
				sh.setStatic(SERVICE_NAME, service);
				sh.setStatic(THREAD_TYPE, "Main");
				return sh.createFactory();
			}
		}
		else if (threadName.startsWith("Proxy:")) {
			String service = serviceName(threadName);
			if (threadName.equals("Proxy:" + service)) {
				SampleSchema sh = root.createDerivedScheme();
				sh.setStatic(SERVICE_NAME, service);
				sh.setStatic(THREAD_TYPE, "Main");
				return sh.createFactory();
			}
			else if (threadName.startsWith("Proxy:" + service + ":TcpAcceptorWorker")) {
				SampleSchema sh = root.createDerivedScheme();
				sh.setStatic(SERVICE_NAME, service);
				sh.setStatic(THREAD_TYPE, "Worker");
				return sh.createFactory();				
			}
			else if (threadName.equals("Proxy:" + service + ":TcpAcceptor:TcpProcessor")) {
				SampleSchema sh = root.createDerivedScheme();
				sh.setStatic(SERVICE_NAME, service);
				sh.setStatic(THREAD_TYPE, "TcpProcessor");
				return sh.createFactory();				
			}
		}
		for(String service: services) {
			if (threadName.startsWith(service + "Worker:")) {
				SampleSchema sh = root.createDerivedScheme();
				sh.setStatic(SERVICE_NAME, service);
				sh.setStatic(THREAD_TYPE, "Worker");
				return sh.createFactory();
			}
		}
		return null;
	}
	
	private static String serviceName(String threadName) {
		int n = threadName.indexOf(':');
		int m = threadName.substring(n + 1).indexOf(':') + n + 1;
		if (m <= n) {
			m = threadName.length();
		}
		return threadName.substring(n + 1, m);
	}	

	private static final class SchemaEnricher implements SchemaConfigurer<MBeanServerConnection>, Serializable {

		private static final long serialVersionUID = 20121116L;
		
		private final SchemaConfigurer<MBeanServerConnection> nested;
		
		public SchemaEnricher(SchemaConfigurer<MBeanServerConnection> nested) {
			this.nested = nested;
		}

		@Override
		public SampleSchema configure(MBeanServerConnection target, SampleSchema root) {
			SampleSchema ss = root.createDerivedScheme();
			ss.setStatic(JvmId.ID, RuntimeMXStruct.get(target).getName());
			return nested.configure(target, ss);
		}
	}
	
	
	private static class SamplerProvider implements SamplerPrototype<JavaThreadStatsSampler>, Serializable {

		private static final long serialVersionUID = 20121113L;
		
		private final Object producerId;
		
		public SamplerProvider(Object producerId) {
			this.producerId = producerId;
		}

		@Override
		public JavaThreadStatsSampler instantiate(SampleSchema schema) {
			final SampleSchema root = schema.createDerivedScheme();
			root.declareDynamic(Measure.TIMESTAMP, double.class);
			root.declareDynamic(Measure.DURATION, double.class);
			root.declareDynamic(Measure.MEASURE, double.class);
			root.setStatic(Measure.PRODUCER, producerId);
			
			return new JavaThreadStatsSampler() {

				private Map<String, SampleFactory> factories = new HashMap<String, SampleFactory>();
				private Set<String> services = new HashSet<String>(); 
				
				@Override
				public void report(long startNanos, long finishNanos, long threadId, String threadName, long cpuTime, long userTime, long blockedTime, long blockedCount, long waitTime, long waitCount, long allocated) {
					SampleFactory factory = getFactory(threadName);
					if (factory != null) {
						factory.newSample()
							.setTimeBounds(startNanos, finishNanos)
							.setMeasure(Seconds.fromNanos(cpuTime))
							.submit();
					}					
				}
				
				SampleFactory getFactory(String threadName) {
					if (!factories.containsKey(threadName)) {
						SampleFactory factory = configureCoherenceThreadSampler(threadName, root, services);
						factories.put(threadName, factory);
					}
					return factories.get(threadName);
				}				
			};
		}
	}
}

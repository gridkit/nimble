package org.gridkit.nimble.monitoring.coherence;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.gridkit.lab.util.jmx.mxstruct.coherence.MemberMBeanLocator;
import org.gridkit.lab.util.jmx.mxstruct.coherence.ServiceMXStruct;
import org.gridkit.lab.util.jmx.mxstruct.common.RuntimeMXStruct;
import org.gridkit.nimble.driver.Activity;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleFactory;
import org.gridkit.nimble.metering.SampleKey;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.metering.SampleWriter;
import org.gridkit.nimble.monitoring.AbstractMonitoringBundle;
import org.gridkit.nimble.monitoring.NoSchema;
import org.gridkit.nimble.monitoring.PollingBundle;
import org.gridkit.nimble.orchestration.ScenarioBuilder;
import org.gridkit.nimble.orchestration.TimeLine;
import org.gridkit.nimble.pivot.Pivot;
import org.gridkit.nimble.pivot.display.DisplayBuilder;
import org.gridkit.nimble.pivot.display.PrintConfig;
import org.gridkit.nimble.probe.jmx.JmxLocator;
import org.gridkit.nimble.probe.jmx.MBeanConnector;
import org.gridkit.nimble.probe.jmx.MBeanProbe;
import org.gridkit.nimble.probe.jmx.MBeanSampler;
import org.gridkit.nimble.probe.jmx.MBeanTarget;
import org.gridkit.nimble.probe.jmx.threading.JavaThreadStatsSampler;
import org.gridkit.nimble.probe.jmx.threading.JavaThreadingProbe;
import org.gridkit.nimble.probe.probe.MonitoringDriver;
import org.gridkit.nimble.probe.probe.SamplerPrototype;
import org.gridkit.nimble.probe.probe.SchemaConfigurer;
import org.gridkit.nimble.statistics.TimeUtils;
import org.gridkit.nimble.util.Seconds;

public class CoherenceCpuMonitoring extends AbstractMonitoringBundle implements CoherenceMonitoringBundle, PollingBundle {

	public static String PACKET_SPEAKER = "PacketSpeaker";
	public static String PACKET_PUBLISHER = "PacketPublisher";
	public static String PACKET_RECEIVER = "PacketReceiver";
	public static String PACKET_LISTENER = "PacketListener";

	private static final String THREAD_WORKER = "Worker";

	private enum SampleAttr implements SampleKey {
		JVM_ID,
		THREAD_ID,
		THREAD_SAMPLE,
		IDLE_SAMPLE,
		BACKLOG_SAMPLE
	}
	
	private static String[] CLUSTER_THREADS = {PACKET_SPEAKER, PACKET_PUBLISHER, PACKET_RECEIVER, PACKET_SPEAKER};
	
	private MBeanConnector connector;
	private SchemaConfigurer<MBeanServerConnection> schemaConfig = new NoSchema<MBeanServerConnection>();
	private long pollPeriod = 5000;
	
	public CoherenceCpuMonitoring(String namespace) {
		super(namespace);
	}

	@Override
	public String getDescription() {
		return "Coherence thread utilization";
	}

	public void setPollPeriod(long millis) {
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
		Activity cpuProbe = pollDriver.deploy(new JmxLocator(connector), new JavaThreadingProbe(), createSchemaConfig(), createSampler(), pollPeriod);
		long samperPeriod = pollPeriod / 10;
		if (samperPeriod < 200) {
			samperPeriod = 200;
		}
		Activity idleProbe = pollDriver.deploy(new MemberMBeanLocator(connector, ServiceMXStruct.NAME), new MBeanProbe(), createServiceMBeanSchemaConfig(), createIdleSampler(), samperPeriod);
		sb.join(phase.getStartCheckpoint());
		
		sb.from(phase.getStopCheckpoint());
		cpuProbe.stop();
		idleProbe.stop();
		sb.join(phase.getDoneCheckpoint());
		
		sb.fromStart();
		cpuProbe.join();
		idleProbe.join();
		sb.join(phase.getDoneCheckpoint());
	}

	@Override
	public void deploy(ScenarioBuilder sb, ServiceProvider context, TimeLine phase) {
		MonitoringDriver pollDriver = context.lookup(MonitoringDriver.class);
		deploy(sb, pollDriver, phase);
	}

	private SchemaConfigurer<MBeanServerConnection> createSchemaConfig() {
		return new ConnentionSchemaEnricher(schemaConfig);
	}

	private SchemaConfigurer<MBeanTarget> createServiceMBeanSchemaConfig() {
		return new ServiceMbeanSchemaEnricher(createSchemaConfig());
	}
	
	private SamplerPrototype<JavaThreadStatsSampler> createSampler() {
		return new ThreadingSamplerProvider(getProducerId());
	}

	private SamplerPrototype<MBeanSampler> createIdleSampler() {
		return new ServiceMBeanSamplerProvider(getProducerId());
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
				
		
		Pivot.Level report = base.level("");
		report
			.calcFrequency(Measure.MEASURE)
			.pivot("cpu")
				.filter(SampleAttr.THREAD_SAMPLE, Boolean.TRUE)
				.calcFrequency(Measure.MEASURE)
				.calcDistinct(SampleAttr.JVM_ID)
				.level("pid")
					.group(SampleAttr.JVM_ID)
						.calcFrequency(Measure.MEASURE)			
						.calcDistinct(SampleAttr.THREAD_ID);
		report
			.pivot("pool")
				.filter(SampleAttr.IDLE_SAMPLE, Boolean.TRUE)
				.calcFrequency(Measure.MEASURE)
				.level("pid")
				.group(SampleAttr.JVM_ID)
					.calcFrequency(Measure.MEASURE);			
		report
			.pivot("queue")
				.filter(SampleAttr.BACKLOG_SAMPLE, Boolean.TRUE)
				.calcFrequency(Measure.MEASURE)
				.level("pid")
					.group(SampleAttr.JVM_ID)
					.calcFrequency(Measure.MEASURE);			
	}

	@Override
	public void configurePrinter(PrintConfig printer) {
		
		printConfig.replay(printer);
		
		DisplayBuilder.with(printer, namespace)
			.constant("Metric", "CPU usage [100% = core]")
			.attribute("Service", SERVICE_NAME)
			.attribute("Thread type",  THREAD_TYPE)
			.deco("cpu").frequency().caption("All processes CPU [%%]").asPercent()
			.deco("pool", "pid").calc().mean().frequency().caption("Pool usage [%%]").asPercent()
			.deco("queue", "pid").calc().mean().frequency().caption("Avg. backlog")
			.deco("cpu", "pid").calc().max().frequency().caption("Max. CPU per process [%%]").asPercent()
			.deco("pool", "pid").calc().max().frequency().caption("Max. pool usage per process [%%]").asPercent()
			.deco("queue", "pid").calc().max().frequency().caption("Max. avg. backlog per process")
			.deco("cpu", "pid").calc().mean().frequency().caption("Avg. CPU per process [%%]").asPercent()
			.deco("cpu", "pid").calc().min().frequency().caption("Min. CPU per process [%%]").asPercent()
			.deco("pool", "pid").calc().min().frequency().caption("Min. pool usage per process [%%]").asPercent()
			.deco("queue", "pid").calc().min().frequency().caption("Min. avg. backlog per process")
			.deco("cpu", "pid").calc().mean().distinct(SampleAttr.THREAD_ID).caption("Avg. thread count")
			.deco("cpu").distinct(SampleAttr.JVM_ID).caption("Processes [N]")
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
				sh.setStatic(THREAD_TYPE, THREAD_WORKER);
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
				sh.setStatic(THREAD_TYPE, THREAD_WORKER);
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

	private static final class ConnentionSchemaEnricher implements SchemaConfigurer<MBeanServerConnection>, Serializable {

		private static final long serialVersionUID = 20121116L;
		
		private final SchemaConfigurer<MBeanServerConnection> nested;
		
		public ConnentionSchemaEnricher(SchemaConfigurer<MBeanServerConnection> nested) {
			this.nested = nested;
		}

		@Override
		public SampleSchema configure(MBeanServerConnection target, SampleSchema root) {
			SampleSchema ss = root.createDerivedScheme();
			ss.setStatic(SampleAttr.JVM_ID, RuntimeMXStruct.get(target).getName());
			return nested.configure(target, ss);
		}
	}

	private static final class ServiceMbeanSchemaEnricher implements SchemaConfigurer<MBeanTarget>, Serializable {
		
		private static final long serialVersionUID = 20121116L;
		
		private final SchemaConfigurer<MBeanServerConnection> nested;
		
		public ServiceMbeanSchemaEnricher(SchemaConfigurer<MBeanServerConnection> nested) {
			this.nested = nested;
		}
		
		@Override
		public SampleSchema configure(MBeanTarget target, SampleSchema root) {
			SampleSchema ss = root.createDerivedScheme();
			ss.setStatic(CoherenceMetricsKey.THREAD_TYPE, THREAD_WORKER);
			ss.setStatic(CoherenceMetricsKey.SERVICE_NAME, target.getMbeanName().getKeyProperty("name"));
			
			return nested.configure(target.getConnection(), ss);
		}
	}
	
	private static class ThreadingSamplerProvider implements SamplerPrototype<JavaThreadStatsSampler>, Serializable {

		private static final long serialVersionUID = 20121113L;
		
		private final Object producerId;
		
		public ThreadingSamplerProvider(Object producerId) {
			this.producerId = producerId;
		}

		@Override
		public JavaThreadStatsSampler instantiate(SampleSchema schema) {
			final SampleSchema root = schema.createDerivedScheme();
			root.declareDynamic(Measure.TIMESTAMP, double.class);
			root.declareDynamic(Measure.DURATION, double.class);
			root.declareDynamic(Measure.MEASURE, double.class);
			root.declareDynamic(SampleAttr.THREAD_ID, long.class);
			root.setStatic(Measure.PRODUCER, producerId);
			root.setStatic(SampleAttr.THREAD_SAMPLE, Boolean.TRUE);
			
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
							.set(SampleAttr.THREAD_ID, threadId)
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
	
	private static class ServiceMBeanSamplerProvider implements SamplerPrototype<MBeanSampler>, Serializable { 
		
		private static final long serialVersionUID = 20121113L;

		private final Object producerId;
		
		private ServiceMBeanSamplerProvider(Object producerId) {
			this.producerId = producerId;
		}

		@Override
		public MBeanSampler instantiate(SampleSchema schema) {
			SampleSchema ss = schema.createDerivedScheme();
			ss.setStatic(Measure.PRODUCER, producerId);
			ss.declareDynamic(Measure.TIMESTAMP, double.class);
			ss.declareDynamic(Measure.DURATION, double.class);
			ss.declareDynamic(Measure.MEASURE, double.class);
			final SampleFactory utilizationFactory = ss.createDerivedScheme().setStatic(SampleAttr.IDLE_SAMPLE, Boolean.TRUE).createFactory();
			final SampleFactory backlogFactory = ss.createDerivedScheme().setStatic(SampleAttr.BACKLOG_SAMPLE, Boolean.TRUE).createFactory();
			return new MBeanSampler() {
				
				private long prevTimestamp;
				private boolean first = true;
				
				@Override
				public void report(MBeanServerConnection connection, ObjectName target) {
					try {
						ServiceMXStruct mstruct = ServiceMXStruct.PROTO.read(connection, target);
						long lastTimestamp = mstruct.getMXStructTimestamp();
						
						if (!first) {
							int idle = mstruct.getThreadIdleCount();
							int total = mstruct.getThreadCount();
							long tx = lastTimestamp - prevTimestamp;
							double time = TimeUtils.toSeconds(tx);
							SampleWriter bl = backlogFactory.newSample();
							bl.setTimeBounds(prevTimestamp, lastTimestamp);
							bl.setMeasure(time * mstruct.getTaskBacklog());
							bl.submit();

							if (idle >= 0 && total != 0 && idle != total) {
								double u = (double)(total - idle) / total;
								SampleWriter util = utilizationFactory.newSample();
								util.setTimeBounds(prevTimestamp, lastTimestamp);
								util.setMeasure(time * u);
								util.submit();
							}
						}
						prevTimestamp = lastTimestamp;
						first = false;
					} catch (Exception e) {
						throw new RuntimeException(e);
					}					
				}
			};
		}
	}
}

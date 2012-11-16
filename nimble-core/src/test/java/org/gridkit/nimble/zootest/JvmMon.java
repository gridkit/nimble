package org.gridkit.nimble.zootest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import javax.management.MBeanServerConnection;

import org.gridkit.lab.jvm.attach.AttachManager;
import org.gridkit.lab.jvm.attach.JavaProcessId;
import org.gridkit.lab.jvm.attach.JavaProcessMatcher;
import org.gridkit.lab.jvm.attach.PatternJvmMatcher;
import org.gridkit.lab.util.jmx.mxstruct.common.RuntimeMXStruct;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nimble.driver.MeteringDriver;
import org.gridkit.nimble.driver.PivotMeteringDriver;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleFactory;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.metering.SpanSamplerTemplate;
import org.gridkit.nimble.orchestration.Scenario;
import org.gridkit.nimble.orchestration.ScenarioBuilder;
import org.gridkit.nimble.pivot.Filters;
import org.gridkit.nimble.pivot.Pivot;
import org.gridkit.nimble.pivot.PivotReporter;
import org.gridkit.nimble.pivot.display.DisplayBuilder;
import org.gridkit.nimble.pivot.display.PivotPrinter2;
import org.gridkit.nimble.print.PrettyPrinter;
import org.gridkit.nimble.probe.PidProvider;
import org.gridkit.nimble.probe.SamplerFactory;
import org.gridkit.nimble.probe.jmx.AttachMBeanConnector;
import org.gridkit.nimble.probe.jmx.MBeanConnector;
import org.gridkit.nimble.probe.jmx.threading.JavaThreadStatsSampler;
import org.gridkit.nimble.probe.probe.JmxProbes;
import org.gridkit.nimble.probe.probe.MonitoringDriver;
import org.gridkit.nimble.probe.probe.Monitoring;
import org.gridkit.nimble.probe.probe.SamplerPrototype;
import org.gridkit.nimble.probe.probe.SchemaConfigurer;
import org.gridkit.nimble.probe.sigar.SigarDriver;
import org.gridkit.nimble.probe.sigar.SigarMeasure;
import org.gridkit.nimble.probe.sigar.StandardSigarSamplerFactoryProvider;
import org.gridkit.nimble.statistics.TimeUtils;
import org.gridkit.vicluster.ViManager;
import org.junit.Test;

public class JvmMon {
	
	private static Object NODE_NAME = "NODE_NAME";
	
	private enum ZooMetrics {
		RUNMETRICS,
		THREAD_STATS,
		GC_STATS,
	}
	
	private ViManager cloud = CloudFactory.createIsolateCloud();
//	private ViManager cloud = CloudFactory.createLocalCloud();

	@Test
	public void testCpuMonitoring() {

		cloud.nodes("mon");		
		
		Pivot pivot = configurePivot();
		PivotMeteringDriver metrics = new PivotMeteringDriver(pivot, 16 << 10);
		
		Scenario scenario = createMonitoringTestScenario(metrics);
		
		scenario.play(cloud);
		
		print(metrics.getReporter());
				
		System.out.println();
		
		System.out.println("Done");
	}

	public void print(PivotReporter repoter) {
		PivotPrinter2 printer = new PivotPrinter2();
		printer.dumpUnprinted();
		
		DisplayBuilder.with(printer)
			.hostname()
			.nodeName();
		
		DisplayBuilder.with(printer, "sigar-cpu-stats")
			.constant("Source", "SIGAR")
			.attribute("Name", SigarMeasure.MEASURE_KEY)
			.attribute("PID", SigarMeasure.PID_KEY)
			.frequency().caption("CPU");
		
		DisplayBuilder.with(printer, "jmx-cpu-stats")
			.constant("Source", "JMX")
			.metricName("Name")
			.attribute("PID", NODE_NAME)
			.frequency().caption("CPU");
		
		DisplayBuilder.with(printer, "run-stats")
			.level("stats").constant("Source", "Task")
			.level("stats").metricName("Name")
			.level("stats").distributionStats().asMillis();

		System.out.println("\n");
			
		PrettyPrinter pp = new PrettyPrinter();		
		pp.print(System.out, printer.print(repoter.getReader()));
		
		PivotPrinter2 cpuOnly = new PivotPrinter2();
		cpuOnly.filter("sigar-cpu-stats", "jmx-cpu-stats");
		cpuOnly.sortByColumn("Node", "Source", "Name");
		
		DisplayBuilder.with(cpuOnly, "sigar-cpu-stats")
			.attribute("Node", NODE_NAME)
			.constant("Source", "SIGAR")
			.attribute("Name", SigarMeasure.MEASURE_KEY)
			.attribute("PID", SigarMeasure.PID_KEY)
			.frequency().caption("CPU")
			.count()
			.duration();
	
		DisplayBuilder.with(cpuOnly, "jmx-cpu-stats")
			.attribute("Node", NODE_NAME)
			.constant("Source", "JMX")
			.metricName("Name")
			.frequency().caption("CPU")
			.count()
			.duration();
		
		System.out.println("\n");
		
		pp = new PrettyPrinter();		
		pp.print(System.out,cpuOnly.print(repoter.getReader()));

		System.out.println("\n");
	}	
	
	private Pivot configurePivot() {
		Pivot pivot = new Pivot();
		
		pivot.root()
			.level("jmx-cpu-stats")
				.filter(Filters.notNull(ZooMetrics.THREAD_STATS))
					.group(MeteringDriver.HOSTNAME)
						.group(MeteringDriver.NODE)
							.group(NODE_NAME)
								.group(Measure.NAME)
									.level("")
										.calcFrequency(Measure.MEASURE)
										.calcDistribution(Measure.DURATION);
		
		pivot.root()
			.level("sigar-cpu-stats")
				.filter(Filters.notNull(SigarMeasure.PROBE_KEY))
				.group(MeteringDriver.HOSTNAME)
					.group(MeteringDriver.NODE)				
						.group(SigarMeasure.PROBE_KEY)
							.group(SigarMeasure.MEASURE_KEY)
								.group(SigarMeasure.PID_KEY)
									.group(NODE_NAME)
										.level("")
											.calcFrequency(Measure.MEASURE)
											.calcDistribution(Measure.DURATION);
												
		
		return pivot;
	}	
	
	private Scenario createMonitoringTestScenario(PivotMeteringDriver metrics) {

        PatternJvmMatcher matcher = new PatternJvmMatcher();
        matcher.matchProp("core-eater", ".*");
		
		ScenarioBuilder sb = new ScenarioBuilder();
		
		MeteringDriver metering = sb.deploy(metrics);
		
        SigarDriver sigar = sb.deploy("mon", new SigarDriver.Impl(2, 600));
        
        PidProvider provider = new JavaPidProvider(matcher);

        sigar.monitorProcCpu(provider, metering.bind(new SigarCpuSamplerProvider()));

        MonitoringDriver pollDriver = Monitoring.deployDriver("mon", sb, metering);
        
        MBeanConnector connector = new AttachMBeanConnector(matcher);

        SamplerPrototype<JavaThreadStatsSampler> threadSampler = JmxProbes.combine(
        	new TotalCpuSamplerProto(),
        	new SysCpuSamplerProto()
        );
        JmxProbes.deployJavaThreadProbe(pollDriver, connector, new NodeClassifier(), threadSampler);
        
		sb.checkpoint("test-start");

		SpanSamplerTemplate t = new SpanSamplerTemplate();
		t.setStatic(ZooMetrics.RUNMETRICS, true);
		t.setStatic(Measure.NAME, "Reader");

		sb.sleep(30000);

		sb.checkpoint("test-finish");
		
		metering.flush();
		
		Scenario scenario = sb.getScenario();
		return scenario;
	}

	@SuppressWarnings("serial")
	private static class JavaPidProvider implements PidProvider, Serializable {

		private final JavaProcessMatcher matcher;
		
		public JavaPidProvider(JavaProcessMatcher matcher) {
			this.matcher = matcher;
		}

		@Override
		public Collection<Long> getPids() {
			List<Long> pids = new ArrayList<Long>();
			for(JavaProcessId jpid: AttachManager.listJavaProcesses(matcher)) {
				pids.add(jpid.getPID());
			}
			return pids;
		}
	}
	
	@SuppressWarnings("serial")
	private static class NodeClassifier implements SchemaConfigurer<MBeanServerConnection>, Serializable {

		@Override
		public SampleSchema configure(MBeanServerConnection target, SampleSchema root) {
			RuntimeMXStruct rmx = RuntimeMXStruct.get(target);
			SampleSchema deriv = root.createDerivedScheme();
			deriv.setStatic(NODE_NAME, "core-eater=" + rmx.getSystemProperties().get("core-eater"));
			deriv.freeze();
			return deriv;
		}
	}
	
	@SuppressWarnings("serial")
	private static class TotalCpuSamplerProto implements SamplerPrototype<JavaThreadStatsSampler>, Serializable {

		@Override
		public JavaThreadStatsSampler instantiate(SampleSchema schema) {
			SampleSchema deriv = schema.createDerivedScheme();
			deriv.setStatic(ZooMetrics.THREAD_STATS, true);
			deriv.setStatic(Measure.NAME, "all:total");
			deriv.declareDynamic(Measure.TIMESTAMP, double.class);
			deriv.declareDynamic(Measure.DURATION, double.class);
			deriv.declareDynamic(Measure.MEASURE, double.class);
			final SampleFactory factory = deriv.createFactory();
			
			return new JavaThreadStatsSampler() {
				@Override
				public void report(long startNanos, long finishNanos, long threadId, String threadName, long cpuTime, long userTime, long blockedTime, long blockedCount, long waitTime, long waitCount, long allocated) {
					factory.newSample()
						.setTimeBounds(startNanos, finishNanos)
						.setMeasure(TimeUtils.toSeconds(cpuTime))
						.submit();
				}
			};
		}
	}

	@SuppressWarnings("serial")
	private static class SysCpuSamplerProto implements SamplerPrototype<JavaThreadStatsSampler>, Serializable {
		
		@Override
		public JavaThreadStatsSampler instantiate(SampleSchema schema) {
			SampleSchema deriv = schema.createDerivedScheme();
			deriv.setStatic(ZooMetrics.THREAD_STATS, true);
			deriv.setStatic(Measure.NAME, "all:sys");
			deriv.declareDynamic(Measure.TIMESTAMP, double.class);
			deriv.declareDynamic(Measure.DURATION, double.class);
			deriv.declareDynamic(Measure.MEASURE, double.class);
			final SampleFactory factory = deriv.createFactory();
			
			return new JavaThreadStatsSampler() {
				@Override
				public void report(long startNanos, long finishNanos, long threadId, String threadName, long cpuTime, long userTime, long blockedTime, long blockedCount, long waitTime, long waitCount, long allocated) {
					factory.newSample()
					.setTimeBounds(startNanos, finishNanos)
					.setMeasure(TimeUtils.toSeconds(cpuTime - userTime))
					.submit();
				}
			};
		}
	}
	
	@SuppressWarnings("serial")
	private static class SigarCpuSamplerProvider extends StandardSigarSamplerFactoryProvider {

		@Override
		public SamplerFactory getProcCpuSampleFactory(long pid) {
	        SampleSchema schema = getGlobalSchema().createDerivedScheme();
	        
	        Properties props = AttachManager.getDetails(pid).getSystemProperties();
	        String nodename = "core-eater=" + props.getProperty("core-eater"); 
	        
	        schema.setStatic(SigarMeasure.PROBE_KEY, SigarMeasure.PROC_CPU_PROBE);
	        schema.setStatic(SigarMeasure.PID_KEY, pid);
	        schema.setStatic(NODE_NAME, nodename);
	        
	        return newProcSampleFactory(pid, schema);
		}		
	}	
}

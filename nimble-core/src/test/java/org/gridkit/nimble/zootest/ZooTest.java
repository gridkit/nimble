package org.gridkit.nimble.zootest;

import java.io.Serializable;

import javax.management.MBeanServerConnection;

import org.gridkit.lab.jvm.attach.PatternJvmMatcher;
import org.gridkit.lab.util.jmx.mxstruct.common.RuntimeMXStruct;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nimble.driver.Activity;
import org.gridkit.nimble.driver.ExecutionDriver;
import org.gridkit.nimble.driver.ExecutionHelper;
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
import org.gridkit.nimble.probe.probe.MetricsPollDriver;
import org.gridkit.nimble.probe.probe.PollProbeHelper;
import org.gridkit.nimble.probe.probe.SamplerPrototype;
import org.gridkit.nimble.probe.probe.SchemaConfigurer;
import org.gridkit.nimble.probe.sigar.SigarDriver;
import org.gridkit.nimble.probe.sigar.SigarMeasure;
import org.gridkit.nimble.probe.sigar.StandardSigarSamplerFactoryProvider;
import org.gridkit.nimble.util.JvmOps;
import org.gridkit.vicluster.ViManager;
import org.junit.After;
import org.junit.Test;

import com.sun.tools.attach.VirtualMachineDescriptor;

public class ZooTest {

	private static Object NODE_NAME = "NODE_NAME";
	
	private enum ZooMetrics {
		RUNMETRICS,
		THREAD_STATS,
		GC_STATS,
	}
	
//	private ViManager cloud = CloudFactory.createIsolateCloud();
	private ViManager cloud = CloudFactory.createLocalCloud();
	
	@After
	public void dropCloud() {
		cloud.shutdown();
	}
	
//	@Test
//	public void testMonitoringTwice() {
//		testMonitoring();
//		testMonitoring();
//	}
	
	@Test
	public void testMonitoring() {

		cloud.nodes("node11", "node22");		
//		cloud.nodes("node11", "node12", "node13", "node14", "node22");		
		
		Pivot pivot = configurePivot();
		PivotMeteringDriver metrics = new PivotMeteringDriver(pivot, 16 << 10);
		
		Scenario scenario = createMonitoringTestScenario(metrics);
		
		scenario.play(cloud);
		
		print(metrics.getReporter());
				
		System.out.println();
//		PivotDumper.dump(metrics.getReporter());
		
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
			.frequency().caption("CPU");
	
		DisplayBuilder.with(cpuOnly, "jmx-cpu-stats")
			.attribute("Node", NODE_NAME)
			.constant("Source", "JMX")
			.metricName("Name")
			.frequency().caption("CPU");
		
		System.out.println("\n");
		
		pp = new PrettyPrinter();		
		pp.print(System.out,cpuOnly.print(repoter.getReader()));

		System.out.println("\n");
	}
	
	@Test
	public void testScopeDerivation() {
		
		cloud.nodes("node11", "node12", "node22");		
		
		Pivot pivot = configurePivot();
		PivotMeteringDriver metrics = new PivotMeteringDriver(pivot, 16 << 10);
		
		Scenario scenario = createComplexDependencyTestScenario(metrics);
		
		scenario.play(cloud);
				
		System.out.println("Done");
	}

	private Pivot configurePivot() {
		Pivot pivot = new Pivot();
		
		pivot.root()
			.level("run-stats")
				.filter(Filters.notNull(ZooMetrics.RUNMETRICS))
				.group(MeteringDriver.HOSTNAME)
					.group(MeteringDriver.NODE)
						.group(Measure.NAME)
							.level("stats")
								.calcDistribution(Measure.MEASURE);
		pivot.root()
			.level("jmx-cpu-stats")
				.filter(Filters.notNull(ZooMetrics.THREAD_STATS))
					.group(MeteringDriver.HOSTNAME)
						.group(MeteringDriver.NODE)
							.group(NODE_NAME)
								.group(Measure.NAME)
									.level("")
										.calcFrequency(Measure.MEASURE);
		
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
											.calcFrequency(Measure.MEASURE);
		
		return pivot;
	}

	private Scenario createMonitoringTestScenario(PivotMeteringDriver metrics) {
		
		ScenarioBuilder sb = new ScenarioBuilder();
		
		MeteringDriver metering = sb.deploy(metrics);
		ExecutionDriver executor = sb.deploy("node1*", ExecutionHelper.newDriver());
		
		ZooTestDriver zoo = sb.deploy("node1*", new ZooTestDriver.Impl());
		
		Runnable task = zoo.getReader();	
		
        SigarDriver sigar = sb.deploy("node22", new SigarDriver.Impl(2, 100));
        
        PidProvider provider = sigar.newPtqlPidProvider("Exe.Name.ct=java");

        sigar.monitorProcCpu(provider, metering.bind(new SigarCpuSamplerProvider()));

        MetricsPollDriver pollDriver = PollProbeHelper.deployDriver("node22", sb, metering);
        
        PatternJvmMatcher matcher = new PatternJvmMatcher();
        matcher.matchProp("vinode.name", "node1.*");
        MBeanConnector connector = new AttachMBeanConnector(matcher);

        JmxProbes.deployJavaThreadProbe(pollDriver, connector, new NodeClassifier(), new TotalCpuSamplerProto());
        
//		JmxThreadProbe probe = sb.deploy("node22", JmxProbeFactory.newThreadProbe(connector));
//        probe.addSampler(metering.bind(new TotalCpuSamplerProvider()));
//        probe.addSampler(metering.bind(new FilteredCpuSamplerProvider()));
		
		sb.checkpoint("test-start");

		SpanSamplerTemplate t = new SpanSamplerTemplate();
		t.setStatic(ZooMetrics.RUNMETRICS, true);
		t.setStatic(Measure.NAME, "Reader");

		Activity run = executor.start(task, ExecutionHelper.constantRateExecution(10, 1, true), metering.bind(t));
		
		zoo.newSample(metering);
		
		sb.sleep(10000);

		run.stop();
		
		sb.checkpoint("test-finish");
		
		metering.flush();
		
		sb.fromStart();
		run.join();
		sb.join("test-finish");
		
		Scenario scenario = sb.getScenario();
		return scenario;
	}
	
	private Scenario createComplexDependencyTestScenario(PivotMeteringDriver metrics) {
		
		ScenarioBuilder sb = new ScenarioBuilder();
		
		MeteringDriver metering = sb.deploy(metrics);
		ExecutionDriver executor = sb.deploy("**", ExecutionHelper.newDriver());
		
		ZooTestDriver zoo1 = sb.deploy("node1*", new ZooTestDriver.Impl());
		ZooTestDriver zoo2 = sb.deploy("node2*", new ZooTestDriver.Impl());
		
		Runnable task1 = zoo1.getReader();	
		Runnable task2 = zoo2.getReader();	
		
		sb.checkpoint("test-start");

		SpanSamplerTemplate t = new SpanSamplerTemplate();
		t.setStatic(ZooMetrics.RUNMETRICS, true);
		t.setStatic(Measure.NAME, "Reader");

		Activity run1 = executor.start(task1, ExecutionHelper.constantRateExecution(10, 1, true), metering.bind(t));
		Activity run2 = executor.start(task2, ExecutionHelper.constantRateExecution(10, 1, true), metering.bind(t));
		
		zoo1.newSample(metering);
		zoo2.newSample(metering);
		
		sb.sleep(1000);

		run1.stop();
		run2.stop();
		
		sb.checkpoint("test-finish");
		
		metering.flush();
		
		sb.fromStart();
		run1.join();
		run2.join();
		sb.join("test-finish");
		
		Scenario scenario = sb.getScenario();
		return scenario;
	}	
	
	private static class NodeClassifier implements SchemaConfigurer<MBeanServerConnection>, Serializable {

		@Override
		public SampleSchema configure(MBeanServerConnection target, SampleSchema root) {
			RuntimeMXStruct rmx = RuntimeMXStruct.get(target);
			SampleSchema deriv = root.createDerivedScheme();
			deriv.setStatic(NODE_NAME, rmx.getSystemProperties().get("vinode.name"));
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
			deriv.declareDynamic(Measure.TIMESTAMP, double.class);
			deriv.declareDynamic(Measure.DURATION, double.class);
			deriv.declareDynamic(Measure.MEASURE, double.class);
			final SampleFactory factory = deriv.createFactory();
			
			return new JavaThreadStatsSampler() {
				@Override
				public void report(long startNanos, long finishNanos, long threadId, String threadName, long cpuTime, long userTime, long blockedTime, long blockedCount, long waitTime, long waitCount, long allocated) {
					factory.newSample()
						.setTimeBounds(startNanos, finishNanos)
						.setMeasure(cpuTime)
						.submit();
				}
			};
		}
	}

	@SuppressWarnings("serial")
	private static class FilteredCpuSamplerProvider implements SamplerPrototype<JavaThreadStatsSampler>, Serializable {
		
		@Override
		public JavaThreadStatsSampler instantiate(SampleSchema schema) {
			SampleSchema deriv = schema.createDerivedScheme();
			deriv.setStatic(ZooMetrics.THREAD_STATS, true);
			deriv.declareDynamic(Measure.TIMESTAMP, double.class);
			deriv.declareDynamic(Measure.DURATION, double.class);
			deriv.declareDynamic(Measure.MEASURE, double.class);
			final SampleFactory factory = deriv.createFactory();
			
			return new JavaThreadStatsSampler() {
				@Override
				public void report(long startNanos, long finishNanos, long threadId, String threadName, long cpuTime, long userTime, long blockedTime, long blockedCount, long waitTime, long waitCount, long allocated) {
					if (threadName.startsWith("pool")) {
						factory.newSample()
							.setTimeBounds(startNanos, finishNanos)
							.setMeasure(cpuTime)
							.submit();
					}
				}
			};
		}
	}
	
	private static class SigarCpuSamplerProvider extends StandardSigarSamplerFactoryProvider {

		@Override
		public SamplerFactory getProcCpuSampleFactory(long pid) {
	        SampleSchema schema = getGlobalSchema().createDerivedScheme();
	        
	        String nodename = "pid:" + pid;
	        try {
	        	VirtualMachineDescriptor vmd = JvmOps.getDescriptor(pid);
	        	if (vmd != null) {
	        		nodename = JvmOps.getProps(vmd).getProperty("vinode.name");
	        	}
	        }
	        catch(Exception e) {
	        	// ignore
	        }
	        
	        System.err.println("Node name: " + nodename);
	        
	        schema.setStatic(SigarMeasure.PROBE_KEY, SigarMeasure.PROC_CPU_PROBE);
	        schema.setStatic(SigarMeasure.PID_KEY, pid);
	        schema.setStatic(NODE_NAME, nodename);
	        
	        return newProcSampleFactory(pid, schema);
		}		
	}
}

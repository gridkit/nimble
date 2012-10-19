package org.gridkit.nimble.zootest;

import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nimble.driver.Activity;
import org.gridkit.nimble.driver.ExecutionDriver;
import org.gridkit.nimble.driver.ExecutionHelper;
import org.gridkit.nimble.driver.MeteringDriver;
import org.gridkit.nimble.driver.PivotMeteringDriver;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.MeteringTemplate;
import org.gridkit.nimble.orchestration.Scenario;
import org.gridkit.nimble.orchestration.ScenarioBuilder;
import org.gridkit.nimble.pivot.Pivot;
import org.gridkit.nimble.pivot.PivotPrinter;
import org.gridkit.nimble.print.PrettyPrinter;
import org.gridkit.nimble.probe.PidProvider;
import org.gridkit.nimble.probe.sigar.SigarDriver;
import org.gridkit.vicluster.ViManager;
import org.junit.After;
import org.junit.Test;

public class ZooTest {

//	private ViManager cloud = IsolateCloudFactory.createCloud("org.gridkit");
	private ViManager cloud = CloudFactory.createLocalCloud();
	
	@After
	public void dropCloud() {
		cloud.shutdown();
	}
	
	@Test
	public void testMonitoring() {

		cloud.nodes("node11", "node12", "node22");		
		
		Pivot pivot = configurePivot();
		PivotMeteringDriver metrics = new PivotMeteringDriver(pivot, 16 << 10);
		
		Scenario scenario = createMonitoringTestScenario(metrics);
		
		scenario.play(cloud);
		
		PivotPrinter printer = new PivotPrinter(pivot, metrics.getReporter());
		
		PrettyPrinter pp = new PrettyPrinter();		
		pp.print(System.out, printer);
		System.out.println();
//		PivotDumper.dump(metrics.getReporter());
		
		System.out.println("Done");
	}

	@Test
	public void testScopeDerivation() {
		
		cloud.nodes("node11", "node12", "node22");		
		
		Pivot pivot = configurePivot();
		PivotMeteringDriver metrics = new PivotMeteringDriver(pivot, 16 << 10);
		
		Scenario scenario = createComplexDependencyTestScenario(metrics);
		
		scenario.play(cloud);
		
		PivotPrinter printer = new PivotPrinter(pivot, metrics.getReporter());
		
		PrettyPrinter pp = new PrettyPrinter();		
		pp.print(System.out, printer);
		System.out.println();
//		PivotDumper.dump(metrics.getReporter());
		
		System.out.println("Done");
	}

	private Pivot configurePivot() {
		Pivot pivot = new Pivot();
		
		pivot.root()
			.group(MeteringDriver.NODE)
				.group(Measure.NAME)
					.level("stats")
						.show()
						.display(MeteringDriver.NODE)
						.display(Measure.NAME)
						.calcDistribution(Measure.MEASURE)
						.displayDistribution(Measure.MEASURE);
		
		return pivot;
	}

	private Scenario createMonitoringTestScenario(PivotMeteringDriver metrics) {
		
		ScenarioBuilder sb = new ScenarioBuilder();
		
		MeteringDriver metering = sb.deploy(metrics);
		ExecutionDriver executor = sb.deploy("node1*", ExecutionHelper.newDriver());
		
		ZooTestDriver zoo = sb.deploy("node1*", new ZooTestDriver.Impl());
		
		Runnable task = zoo.getReader();	
		
        SigarDriver sigar = sb.deploy("**", new SigarDriver.Impl(2, 100));
        
        PidProvider provider = sigar.newPtqlPidProvider("Exe.Name.ct=java");

        sigar.monitorProcCpu(provider, metering);
		
		sb.checkpoint("test-start");

		MeteringTemplate t = new MeteringTemplate();
		t.setStatic(Measure.NAME, "Reader");

		Activity run = executor.start(task, ExecutionHelper.constantRateExecution(10, 1, true), metering, t);
		
		zoo.newSample(metering);
		
		sb.sleep(1000);

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

		MeteringTemplate t = new MeteringTemplate();
		t.setStatic(Measure.NAME, "Reader");

		Activity run1 = executor.start(task1, ExecutionHelper.constantRateExecution(10, 1, true), metering, t);
		Activity run2 = executor.start(task2, ExecutionHelper.constantRateExecution(10, 1, true), metering, t);
		
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
}

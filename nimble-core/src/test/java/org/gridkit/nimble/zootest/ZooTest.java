package org.gridkit.nimble.zootest;

import java.util.concurrent.TimeUnit;

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
import org.gridkit.vicluster.telecontrol.isolate.IsolateCloudFactory;
import org.junit.After;
import org.junit.Test;

public class ZooTest {

	private ViManager cloud = IsolateCloudFactory.createCloud("org.gridkit");
	
	@After
	public void dropCloud() {
		cloud.shutdown();
	}
	
	@Test
	public void test() {

		cloud.nodes("node11", "node12", "node22");		
		
		Pivot pivot = configurePivot();
		PivotMeteringDriver metrics = new PivotMeteringDriver(pivot, 1024);
		
		Scenario scenario = createTestScenario(metrics);
		
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

	private Scenario createTestScenario(PivotMeteringDriver metrics) {
		
		ScenarioBuilder sb = new ScenarioBuilder();
		
		MeteringDriver metering = sb.deploy(metrics);
		ExecutionDriver executor = sb.deploy("node1*", ExecutionHelper.newDriver());
		
		ZooTestDriver zoo = sb.deploy("node1*", new ZooTestDriver.Impl());
		
		Runnable task = zoo.getReader();	
		
        SigarDriver sigar = sb.deploy("**", new SigarDriver.Impl(2, 100, TimeUnit.MILLISECONDS));
        
        PidProvider provider = sigar.newPtqlPidProvider("Exe.Name.ct=java");

        sigar.monitorProcCpu(provider, metering);
		
		sb.checkpoint("test-start");

		MeteringTemplate t = new MeteringTemplate();
		t.setStatic(Measure.NAME, "Reader");

		Activity run = executor.start(task, ExecutionHelper.constantRateExecution(100, 20, true), metering, t);
		
		zoo.newSample(metering);
		
		sb.sleep(5000);

		run.stop();
		
		sb.checkpoint("test-finish");
		
		metering.flush();
		
		sb.fromStart();
		run.join();
		sb.join("test-finish");
		
		Scenario scenario = sb.getScenario();
		return scenario;
	}
}

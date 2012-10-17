package org.gridkit.nimble.zootest;

import org.gridkit.nimble.driver.Activity;
import org.gridkit.nimble.driver.ExecutionDriver;
import org.gridkit.nimble.driver.ExecutionHelper;
import org.gridkit.nimble.driver.MeteringDriver;
import org.gridkit.nimble.driver.PivotMeteringDriver;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.orchestration.Scenario;
import org.gridkit.nimble.orchestration.ScenarioBuilder;
import org.gridkit.nimble.pivot.Pivot;
import org.gridkit.nimble.pivot.PivotPrinter;
import org.gridkit.nimble.print.PrettyPrinter;
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
		cloud.node("node11");
		cloud.node("node12");
		cloud.node("node22");
		
		
		Pivot pivot = configurePivot();
		PivotMeteringDriver metrics = new PivotMeteringDriver(pivot, 1024);
		
		Scenario scenario = createTestScenario(metrics);
		
		scenario.play(cloud);
		
		PivotPrinter printer = new PivotPrinter(pivot, metrics.getReporter());
		
		PrettyPrinter pp = new PrettyPrinter();
		
		pp.print(System.out, printer);
		
		System.out.println("Done");
	}

	private Pivot configurePivot() {
		Pivot pivot = new Pivot();
		
		pivot.root()
			.group(MeteringDriver.NODE)
				.group(Measure.NAME)
					.level("stats")
						.show()
						.display(Measure.NAME)
						.display(MeteringDriver.NODE)
						.calcDistribution(Measure.MEASURE)
						.displayDistribution(Measure.MEASURE);
		
		return pivot;
	}

	private Scenario createTestScenario(PivotMeteringDriver metrics) {
		
		ScenarioBuilder sb = new ScenarioBuilder();
		
		MeteringDriver metering = sb.deploy(metrics);
		ExecutionDriver executor = sb.deploy(ExecutionHelper.newDriver());
		
		ZooTestDriver zoo = sb.deploy("node1*", new ZooTestDriver.Impl());
		
		Runnable task = zoo.getReader();		
		
		sb.checkpoint("test-start");
		
		Activity run = executor.start(task, ExecutionHelper.constantRateExecution(2, 10, true), null, null);
		
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

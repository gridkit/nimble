package org.gridkit.nimble.zootest;

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
		cloud.node("node1");
		cloud.node("node2");
		
		
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
		
		PivotMeteringDriver metrics = new PivotMeteringDriver(pivot, 1024);
		
		Scenario scenario = createTestScenario(metrics);
		
		scenario.play(cloud);
		
		PivotPrinter printer = new PivotPrinter(pivot, metrics.getReporter());
		
		PrettyPrinter pp = new PrettyPrinter();
		
		pp.print(System.out, printer);
		
		System.out.println("Done");
	}

	private Scenario createTestScenario(PivotMeteringDriver metrics) {
		
		ScenarioBuilder sb = new ScenarioBuilder();
		
		MeteringDriver metering = sb.deploy("**", metrics);
		ZooTestDriver zoo = sb.deploy("**", new ZooTestDriver.Impl());
		
		sb.checkpoint("test-start");
		
		zoo.newSample(metering);
		
		sb.checkpoint("test-finish");
		
		metering.flush();
		
		Scenario scenario = sb.getScenario();
		return scenario;
	}
}

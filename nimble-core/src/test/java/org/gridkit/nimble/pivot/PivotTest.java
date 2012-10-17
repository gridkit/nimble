package org.gridkit.nimble.pivot;

import org.gridkit.nimble.metering.ArraySampleManager;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleFactory;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.print.PrettyPrinter;
import org.junit.Test;

public class PivotTest {

	@Test
	public void test_pivot() {
		
		Pivot pv = new Pivot();
		pv
			.root()
				.group(Measure.NAME)
					.level("Stats")
					    .show()
						.calcDistribution(Measure.MEASURE)
						.calcFrequency("A", 1)
						.display(Measure.NAME)
						.displayDistribution(Measure.MEASURE)
						.displayThroughput("A")
							.pivot()
								.level("A=0")
								.filter("A", 0)
								.calcDistribution(Measure.MEASURE)
								.show()
									.displayDistribution(Measure.MEASURE, CommonStats.MEAN, CommonStats.COUNT);
			 
		
		ArraySampleManager asm1 = new ArraySampleManager(100);
		ArraySampleManager asm2 = new ArraySampleManager(100);
		
		SampleSchema ss = ArraySampleManager.newScheme();
		ss.declareDynamic(Measure.TIMESTAMP, double.class);
		
		SampleSchema ss1 = ss.createDerivedScheme();
		asm1.adopt(ss1);
		ss1.setStatic(Measure.NAME, "XYZ");
//		ss1.setStatic(Measure.NAME, "ABC");
		ss1.declareDynamic(Measure.MEASURE, double.class);
		ss1.declareDynamic("A", String.class);

		SampleSchema ss2 = ss.createDerivedScheme();
		asm2.adopt(ss2);
		ss2.setStatic(Measure.NAME, "ABC");
		ss2.declareDynamic(Measure.MEASURE, double.class);
		ss2.declareDynamic("A", String.class);
		
		SampleFactory sf1 = ss1.createFactory();
		SampleFactory sf2 = ss2.createFactory();

		generateSamples(sf1, sf2);

		DistributedPivotReporter reporter = new DistributedPivotReporter(pv);
		
		SampleAccumulator sub1 = reporter.createSlaveReporter();
		SampleAccumulator sub2 = reporter.createSlaveReporter();
		
		sub1.accumulate(asm1);
		sub1.flush();

		sub2.accumulate(asm2);
		sub2.flush();
		
		generateSamples(sf1, sf2);
		
		sub1.accumulate(asm1);
		sub1.flush();

		sub2.accumulate(asm2);
		sub2.flush();
		
		reporter.listChildren(LevelPath.root()).get(0);
		
		PrettyPrinter pp = new PrettyPrinter();
		
		PivotPrinter printer = new PivotPrinter(pv, reporter);
		
		pp.print(System.out, printer);
		
		System.out.println("Done");
		
	}

	private void generateSamples(SampleFactory sf1, SampleFactory sf2) {
		for(int i = 0; i != 5; ++i) {
			sf1.newSample()
				.setMeasure(10 + i)
				.set("A", i % 3)
				.set(Measure.TIMESTAMP, i)
				.submit();
			sf2.newSample()
				.setMeasure(8 + i)
				.set("A", i % 2)
				.set(Measure.TIMESTAMP, i)
				.submit();
		}
	}
	
}

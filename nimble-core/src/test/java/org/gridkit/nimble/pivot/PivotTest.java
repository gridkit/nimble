package org.gridkit.nimble.pivot;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.gridkit.nimble.metering.ArraySampleManager;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleFactory;
import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.pivot.display.DisplayBuilder;
import org.gridkit.nimble.pivot.display.DisplayFactory;
import org.gridkit.nimble.pivot.display.PivotPrinter2;
import org.gridkit.nimble.print.PrettyPrinter;
import org.gridkit.nimble.statistics.FrequencySummary;
import org.junit.Test;

public class PivotTest {

	private static final Object ATTR_A = "A";

	@Test
	public void test_pivot() {
		
		Pivot pv = new Pivot();
		
		Pivot.Level pivotLevel = 
		pv
			.root()
				.level("test-metrics")
					.group(Measure.NAME)
						.level("")
						    .show()
							.calcDistribution(Measure.MEASURE)
							.calcFrequency(ATTR_A, 1)
							.display(Measure.NAME)
							.displayDistribution(Measure.MEASURE)
							.displayThroughput(ATTR_A)
								.pivot();
		
		pivotLevel
			.level("A=0")
			.filter(ATTR_A, 0)
			.calcDistribution(Measure.MEASURE)
			.show()
			.displayDistribution(Measure.MEASURE, CommonStats.MEAN, CommonStats.COUNT);

		pivotLevel
			.level("A=1")
			.filter(ATTR_A, 1)
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
		ss1.declareDynamic(ATTR_A, String.class);

		SampleSchema ss2 = ss.createDerivedScheme();
		asm2.adopt(ss2);
		ss2.setStatic(Measure.NAME, "ABC");
		ss2.declareDynamic(Measure.MEASURE, double.class);
		ss2.declareDynamic(ATTR_A, String.class);
		
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
		
//		dump(reporter.getReader());
		
		PivotPrinter2 p2 = new PivotPrinter2();
		p2.dumpUnprinted();
		p2.add(DisplayFactory.attribute("Name", Measure.NAME));
		p2.add(DisplayFactory.distributionStats(Measure.MEASURE));
		p2.add(DisplayFactory.genericStats(ATTR_A, CommonStats.FREQUENCY, CommonStats.DURATION));
		p2.add(DisplayFactory.decorated("[A=0] %s", DisplayFactory.genericStats(Measure.MEASURE, CommonStats.MEAN, CommonStats.COUNT), "A=0"));
		p2.add(DisplayFactory.decorated("[A=1] %s", DisplayFactory.genericStats(Measure.MEASURE, CommonStats.MEAN, CommonStats.COUNT), "A=1"));
		
		new PrettyPrinter().print(System.out, p2.print(reporter.getReader()));
		
		System.out.println("\n");
		
		print(pv, reporter);
		
		System.out.println("\nDone");
		
	}

	private void print(Pivot pv, DistributedPivotReporter reporter) {
		PrettyPrinter pp = new PrettyPrinter();
		
		PivotPrinter printer = new PivotPrinter(pv, reporter);
		
		pp.print(System.out, printer);
	}

	private void dump(SampleReader reader) {
		if (reader.isReady() || reader.next()) {
			while(true) {
				List<Object> keys = reader.keySet();
				for(Object key: keys) {
					Object value = reader.get(key);
					System.out.print(key + "=" + format(value) + ", ");
				}
				System.out.println();
				if (!reader.next()) {
					break;
				}
			}
		}
	}

	private Object format(Object value) {
		if (value instanceof StatisticalSummary) {
			return "<distr. summary>";
		}
		else if (value instanceof FrequencySummary) {
			return "<freq. summary>";
		}
		else {
			return value;
		}
	}

	private void generateSamples(SampleFactory sf1, SampleFactory sf2) {
		for(int i = 0; i != 5; ++i) {
			sf1.newSample()
				.setMeasure(10 + i)
				.set(ATTR_A, i % 3)
				.set(Measure.TIMESTAMP, i)
				.submit();
			sf2.newSample()
				.setMeasure(8 + i)
				.set(ATTR_A, i % 2)
				.set(Measure.TIMESTAMP, i)
				.submit();
		}
	}
	
}

package org.gridkit.nimble.pivot;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.gridkit.nimble.metering.ArraySampleManager;
import org.gridkit.nimble.metering.DeltaSampleWriter;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleFactory;
import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.metering.SampleSet;
import org.gridkit.nimble.pivot.display.DisplayBuilder;
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
							.calcDistribution(Measure.MEASURE)
							.calcFrequency(Measure.MEASURE)
							.pivot();
		
		pivotLevel
			.level("A=0")
			.filter(ATTR_A, 0)
			.calcDistribution(Measure.MEASURE);

		pivotLevel
			.level("A=1")
			.filter(ATTR_A, 1)
			.calcDistribution(Measure.MEASURE);
		
		
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
		
		DisplayBuilder.with(p2)
			.metricName()
			.count()
			.distributionStats().caption("%s (ms)").asMillis()
			.frequency()
			.duration()
			.decorated("A=0").stats(Measure.MEASURE, CommonStats.MEAN, CommonStats.COUNT).caption("[A=0] %s")
			.decorated("A=1").stats(Measure.MEASURE, CommonStats.MEAN, CommonStats.COUNT).caption("[A=1] %s")
		;
					
		new PrettyPrinter().print(System.out, p2.print(reporter.getReader()));
		
		System.out.println("\n");
		
		DeltaSampleWriter dsw = new DeltaSampleWriter();
		dsw.addSamples(reporter.getReader());
		SampleSet dsr = reserialize(dsw.createSampleSet());
		
		new PrettyPrinter().print(System.out, p2.print(dsr.reader()));
		
		System.out.println("\nDone");
		
	}

	private SampleSet reserialize(SampleSet set) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(set);
			oos.close();
			byte[] blob = bos.toByteArray();
			System.out.println("Blob size: " + blob.length);
			ByteArrayInputStream bis = new ByteArrayInputStream(blob);
			ObjectInputStream ois = new ObjectInputStream(bis);
			SampleSet set2 = (SampleSet) ois.readObject();
			return set2;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unused")
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

package org.gridkit.nimble.pivot;

import org.gridkit.nimble.metering.ArraySampleManager;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleFactory;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.print.PrettyPrinter;
import org.junit.Test;

import com.sun.tools.internal.ws.processor.model.Message;

public class PivotTest {

	@Test
	public void test_pivot() {
		
		Pivot pv = new Pivot();
		pv
			.root()
				.group(Measure.NAME)
					.level("Stats")
					    .show()
						.calcGausian(Measure.MEASURE)
						.display(Measure.NAME)
						.displayStats(Measure.MEASURE);
			 
		
		ArraySampleManager asm = new ArraySampleManager(100);
		
		SampleSchema ss = ArraySampleManager.newScheme();
		asm.adopt(ss);
		
		SampleSchema ss1 = ss.createDerivedScheme();
		ss1.setStatic(Measure.NAME, "XYZ");
		ss1.declareDynamic(Measure.MEASURE, double.class);

		SampleSchema ss2 = ss.createDerivedScheme();
		ss2.setStatic(Measure.NAME, "ABC");
		ss2.declareDynamic(Measure.MEASURE, double.class);
		
		SampleFactory sf1 = ss1.createFactory();
		SampleFactory sf2 = ss2.createFactory();
		for(int i = 0; i != 5; ++i) {
			sf1.newSample()
				.setMeasure(10 + i)
				.submit();
			sf2.newSample()
				.setMeasure(8 + i)
				.submit();
		}

		PivotReporter reporter = new PivotReporter(pv);
		
		asm.next();
		reporter.accumulate(asm);
		
		reporter.listChildren(RowPath.root()).get(0);
		
		PrettyPrinter pp = new PrettyPrinter();
		
		PivotPrinter printer = new PivotPrinter(pv, reporter);
		
		pp.print(System.out, printer);
		
		System.out.println("Done");
		
	}
	
}

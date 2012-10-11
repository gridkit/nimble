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
			.level("XXX")
				.filter(Measure.NAME, "XXX")
				.level("Stats")
				    .show()
					.calcGausian(Measure.MEASURE)
					.display(Measure.NAME)
					.displayStats(Measure.MEASURE);
			 
		
		ArraySampleManager asm = new ArraySampleManager(100);
		
		SampleSchema ss = ArraySampleManager.newScheme();
		asm.adopt(ss);
		
		ss.setStatic(Measure.NAME, "XXX");
		ss.declareDynamic(Measure.MEASURE, double.class);
		
		SampleFactory sf = ss.createFactory();
		for(int i = 0; i != 5; ++i) {
			sf.newSample()
				.setMeasure(10 + i)
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

package org.gridkit.nimble.zootest;

import java.io.Serializable;

import org.gridkit.nimble.driver.MeteringDriver;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleFactory;

public interface ZooTestDriver {
	
	public void newSample(MeteringDriver driver);
	
	@SuppressWarnings("serial")
	public static class Impl implements ZooTestDriver, Serializable {

		@Override
		public void newSample(MeteringDriver driver) {
			SampleFactory factory = driver.getSchema()
					.setStatic(Measure.NAME, "test_metric")
					.declareDynamic(Measure.MEASURE, double.class)
					.createFactory();
			
			factory.newSample().setMeasure(100).submit();
		}
	}
}

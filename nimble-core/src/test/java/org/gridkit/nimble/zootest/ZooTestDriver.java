package org.gridkit.nimble.zootest;

import java.io.Serializable;
import java.util.concurrent.locks.LockSupport;

import org.gridkit.nimble.driver.MeteringDriver;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleFactory;

public interface ZooTestDriver {
	
	public void newSample(MeteringDriver driver);
	
	public Runnable getReader();
	
	@SuppressWarnings("serial")
	public static class Impl implements ZooTestDriver, Serializable {

		@Override
		public Runnable getReader() {
			return new Runnable() {
				@Override
				public void run() {
					System.out.println("run");
					LockSupport.parkNanos(1000);
				}
			};
		}

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

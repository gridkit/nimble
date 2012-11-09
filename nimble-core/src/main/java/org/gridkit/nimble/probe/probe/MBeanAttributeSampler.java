package org.gridkit.nimble.probe.probe;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleFactory;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.probe.jmx.MBeanSampler;

public abstract class MBeanAttributeSampler implements Serializable {
	
	private static final long serialVersionUID = 20121108L;
	
	protected final String attribute;
	protected final Map<Object, Object> statics = new LinkedHashMap<Object, Object>();
	
	protected MBeanAttributeSampler(String attribute) {
		this.attribute = attribute;
	}

	public void setStatic(Object key, Object value) {
		statics.put(key, value);
	};
	
	protected Object getValue(MBeanServerConnection conn, ObjectName name) {
		try {
			return conn.getAttribute(name, attribute);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static class Delta extends MBeanAttributeSampler implements SamplerPrototype<MBeanSampler>, Serializable {

		private static final long serialVersionUID = 20121108L;

		private double scale = 1;
		
		public Delta(String attribute) {
			super(attribute);
		}
		
		public void setScale(double scale) {
			this.scale = scale;
		}

		@Override
		public MBeanSampler instantiate(SampleSchema schema) {
			SampleSchema s = schema.createDerivedScheme();
			for(Object key: statics.keySet()) {
				s.setStatic(key, statics.get(key));
			}
			s.declareDynamic(Measure.TIMESTAMP, double.class);
			s.declareDynamic(Measure.DURATION, double.class);
			s.declareDynamic(Measure.MEASURE, double.class);
			
			final SampleFactory factory = s.createFactory();
			return new MBeanSampler() {
				
				boolean first = true;
				long lastTimestamp;
				double lastValue;
				
				@Override
				public void report(MBeanServerConnection connection, ObjectName target) {					
					Object v = getValue(connection, target);
					if (v != null) {
						long timestamp = System.nanoTime();
						double m = ((Number)v).doubleValue();
						if (!first) {
							factory.newSample()
								.setTimeBounds(lastTimestamp, timestamp)
								.setMeasure(scale * (m - lastValue))
								.submit();
						}
						
						first = false;
						lastTimestamp = timestamp;
						lastValue = m;
					}
				}
			};
		}
	}

}

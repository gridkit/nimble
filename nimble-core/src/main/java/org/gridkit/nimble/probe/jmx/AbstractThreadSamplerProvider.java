package org.gridkit.nimble.probe.jmx;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;

import org.gridkit.nimble.driver.MeteringAware;
import org.gridkit.nimble.driver.MeteringDriver;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleFactory;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.metering.SampleWriter;
import org.gridkit.nimble.statistics.TimeUtils;

public abstract class AbstractThreadSamplerProvider implements MeteringAware<JmxAwareSamplerProvider<JavaThreadStatsSampler>>, JmxAwareSamplerProvider<JavaThreadStatsSampler>, Serializable {

	private static final long serialVersionUID = 20121023L;
	
	private SampleSchema schema;
	
	@Override
	public JmxAwareSamplerProvider<JavaThreadStatsSampler> attach(MeteringDriver metering) {
		schema = metering.getSchema();
		return this;
	}
	
	protected SampleSchema configureConnectionSchema(MBeanServerConnection connection, SampleSchema root) {
		return root;
	}
	
	protected SampleSchema configureThreadSchema(String threadName, SampleSchema root) {
		return root;
	}
	
	protected abstract void writeSample(SampleWriter writer, long threadId, double cpuTime, double userTime, double blockedTime, long blockedCount, double waitTime, long waitCount, long allocated);
	
	@Override
	public JavaThreadStatsSampler getSampler(MBeanServerConnection connection) {
		SampleSchema cs = configureConnectionSchema(connection, schema);
		cs = cs.createDerivedScheme();
		
		cs.declareDynamic(Measure.TIMESTAMP, double.class);		
		cs.declareDynamic(Measure.DURATION, double.class);
		cs.freeze();
		
		return new ThreadSampler(cs);
	}
	
	private class ThreadSampler implements JavaThreadStatsSampler {

		private final SampleSchema rootSchema;
		private final SampleFactory factory;
		private final Map<String, SampleFactory> factoryCache = new LinkedHashMap<String, SampleFactory>();  
		
		public ThreadSampler(SampleSchema rootSchema) {
			this.rootSchema = rootSchema;
			this.factory = rootSchema.createFactory();
		}

		@Override
		public synchronized void report(long startNanos, long finishNanos, long threadId, String threadName, long cpuTime, long userTime, long blockedTime, long blockedCount, long waitTime, long waitCount, long allocated) {
			SampleFactory factory;
			if (factoryCache.containsKey(threadName)) {
				factory = factoryCache.remove(threadName);
				factoryCache.put(threadName, factory);
			}
			else {
				SampleSchema sc = configureThreadSchema(threadName, rootSchema);
				if (sc == rootSchema) {
					factory = this.factory;
				}
				else if (sc == null) {
					factory = null;
				}
				else {
					factory = sc.createFactory();
				}
				factoryCache.put(threadName, factory);
				if (factoryCache.size() > 512) {
					factoryCache.entrySet().iterator().remove();
				}
			}
			
			if (factory != null) {
				SampleWriter writer = factory.newSample();
				writer.setTimeBounds(startNanos, finishNanos);
				writeSample(new UnsibmitableSample(writer), 
						threadId, 
						TimeUtils.toSeconds(cpuTime), 
						TimeUtils.toSeconds(userTime), 
						TimeUtils.toSeconds(blockedTime), 
						blockedCount, 
						TimeUtils.toSeconds(waitTime), 
						waitCount, 
						allocated);
				writer.submit();
			}
		}			
	}
	
	private static class UnsibmitableSample implements SampleWriter {
		
		private final SampleWriter delegate;

		public UnsibmitableSample(SampleWriter delegate) {
			this.delegate = delegate;
		}

		public SampleWriter setMeasure(double measure) {
			return delegate.setMeasure(measure);
		}

		public SampleWriter setTimestamp(long timestamp) {
			return delegate.setTimestamp(timestamp);
		}

		public SampleWriter setTimeAndDuration(long startNs, long durationNs) {
			return delegate.setTimeAndDuration(startNs, durationNs);
		}

		public SampleWriter setTimeBounds(long start, long finish) {
			return delegate.setTimeBounds(start, finish);
		}

		public SampleWriter set(Object key, int value) {
			return delegate.set(key, value);
		}

		public SampleWriter set(Object key, long value) {
			return delegate.set(key, value);
		}

		public SampleWriter set(Object key, double value) {
			return delegate.set(key, value);
		}

		public SampleWriter set(Object key, Object value) {
			return delegate.set(key, value);
		}

		public SampleWriter set(Object key, String value) {
			return delegate.set(key, value);
		}

		public void submit() {
			// ignore
		}
	}
}

package org.gridkit.nimble.metering;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.gridkit.nimble.statistics.TimeUtils;

public class ArraySampleManager implements SampleReader {

	public static SampleSchema newScheme() {
		return new Schema((ArraySampleManager)null);
	}
	
	private BlockingQueue<Sample> sampleQueue;
	private Sample nextSample;	
	
	public ArraySampleManager(int bufferSize) {
		this.sampleQueue = new ArrayBlockingQueue<Sample>(bufferSize);
	}
	
	void submit(Sample sample) {
		sampleQueue.add(sample);
	}
	
	public void adopt(SampleSchema schema) {
		if (!(schema instanceof Schema)) {
			throw new IllegalArgumentException("Unsupported rootSchema implementation " + schema);
		}
		else {
			((Schema)schema).manager = this;
		}
	}
	
	public boolean isReady() {
		return nextSample != null;
	}
	
	@Override
	public boolean next() {
		Sample s = sampleQueue.poll();
		if (s == null) {
			nextSample = null;
			return false;
		}
		else {
			nextSample = s;
			return true;
		}
	}

	@Override
	public List<Object> keySet() {
		if (nextSample == null) {
			throw new NoSuchElementException("Call next() to resolve current sample");
		}
		return nextSample.keySet;
	}

	@Override
	public Object get(Object key) {
		if (nextSample == null) {
			throw new NoSuchElementException("Call next() to resolve current sample");
		}
		Object v = nextSample.statics.get(key);
		if (v == null) {
			for(int i = 0; i != nextSample.measureKeys.length; ++i) {
				if (key.equals(nextSample.measureKeys[i])) {
					v = nextSample.measures[i];
					break;
				}
			}
		}
		return v;
	}

	private static class Schema implements SampleSchema, Serializable {

		private static final long serialVersionUID = 20121009L;
		
		private Schema parent;
		private Map<Object, Object> statics = new LinkedHashMap<Object, Object>();
		private Map<Object, Class<?>> measures = new LinkedHashMap<Object, Class<?>>();
		private boolean frozen;
		
		private transient ArraySampleManager manager;
		
		Schema(ArraySampleManager manager) {
			this((Schema)null);
			this.manager = manager;
		}

		Schema(Schema schema) {
			parent = schema;
			if (parent != null) {
				manager = parent.manager;
			}
		}
		
		@Override
		public SampleSchema createDerivedScheme() {
			return new Schema(this);
		}

		@Override
		public synchronized SampleFactory createFactory() {
			if (manager == null) {
				throw new IllegalStateException("Schema instance should be bound to ArraySampleManager");
			}
			return new ArraySampleFactory(this);
		}
		
		@Override
		public synchronized void freeze() {
			this.frozen = true;
		}

		Map<Object, Object> collectStatics() {
			Map<Object, Object> map = new LinkedHashMap<Object, Object>();
			collectStatics(map);
			return map;
		}
		
		void collectStatics(Map<Object, Object> map) {
			if (parent != null) {
				parent.collectStatics(map);
			}
			map.putAll(statics);			
		}
		
		Map<Object, Class<?>> collectMeasures() {
			Map<Object, Class<?>> map = new LinkedHashMap<Object, Class<?>>();
			collectMeasures(map);
			return map;			
		}

		void collectMeasures(Map<Object, Class<?>> map) {
			if (parent != null) {
				parent.collectMeasures(map);
			}
			map.putAll(measures);			
		}
		
		synchronized boolean containsKey(Object key) {
			if (parent != null && parent.containsKey(key)) {
				return true;
			}
			else if (statics.containsKey(key)) {
				return true;
			}
			else {
				return measures.containsKey(key);
			}
		}
		
		@Override
		public synchronized SampleSchema setStatic(Object key, Object value) {
			if (frozen) {
				throw new IllegalStateException("Schema is frozen");
			}
			if (containsKey(key)) {
				throw new IllegalArgumentException("Attribute: " + key + " is already present");
			}
			statics.put(key, value);
			return this;
		}

		@Override
		public SampleSchema declareDynamic(Object key, Class<?> type) {
			if (frozen) {
				throw new IllegalStateException("Schema is frozen");
			}			
			if (containsKey(key)) {
				throw new IllegalArgumentException("Attribute: " + key + " is already present");
			}
			measures.put(key, type);
			return this;
		}
	}
	
	private static class ArraySampleFactory implements SampleFactory {
		
		private Map<Object, Object> statics;
		private List<Object> keySet;
		private Object[] measureKeys;
		
		private ArraySampleManager manager;
		
		public ArraySampleFactory(Schema schema) {
			this.manager = schema.manager;
			
			statics = schema.collectStatics();
			measureKeys = schema.collectMeasures().keySet().toArray();
			keySet = new ArrayList<Object>(statics.size() + measureKeys.length);
			keySet.addAll(statics.keySet());
			keySet.addAll(Arrays.asList(measureKeys));
			keySet = Collections.unmodifiableList(keySet);
		}

		@Override
		public SampleWriter newSample() {
			return new ArraySampleWriter(manager, keySet, statics, measureKeys);
		}
	}

	private static class ArraySampleWriter implements SampleWriter {
		
		private ArraySampleManager manager;
		private List<Object> keySet;
		private Map<Object, Object> statics;
		private Object[] measureKeys;
		private Object[] measures;
		
		public ArraySampleWriter(ArraySampleManager manager, List<Object> keySet, Map<Object, Object> statics, Object[] measureKeys) {
			this.manager = manager;
			this.keySet = keySet;
			this.statics = statics;
			this.measureKeys = measureKeys;
			this.measures = new Object[measureKeys.length];
		}

		@Override
		public SampleWriter setMeasure(double measure) {
			internalSet(Measure.MEASURE, measure);
			return this;
		}

		@Override
		public SampleWriter setTimestamp(long timestamp) {
			internalSet(Measure.TIMESTAMP, TimeUtils.normalize(timestamp));
			return this;
		}

		@Override
		public SampleWriter setTimeBounds(long start, long finish) {
			internalSet(Measure.TIMESTAMP, TimeUtils.normalize(start));
			internalSet(Measure.END_TIMESTAMP, TimeUtils.normalize(finish));
			return this;
		}

		@Override
		public SampleWriter set(Object key, int value) {
			internalSet(key, value);
			return this;
		}

		@Override
		public SampleWriter set(Object key, long value) {
			internalSet(key, value);
			return this;
		}

		@Override
		public SampleWriter set(Object key, double value) {
			internalSet(key, value);
			return this;
		}

		@Override
		public SampleWriter set(Object key, Object value) {
			internalSet(key, value);
			return this;
		}

		protected void internalSet(Object key, Object value) {
			for(int i = 0; i != measureKeys.length; ++i) {
				if (key.equals(measureKeys[i])) {
					measures[i] = value;
					return;
				}
			}
			throw new IllegalArgumentException("Unknown attribute '" + key + "'");
		}

		@Override
		public SampleWriter set(Object key, String value) {
			internalSet(key, (Object) value);
			return this;
		}

		@Override
		public void submit() {
			Sample s = new Sample(keySet, statics, measureKeys, measures);
			manager.submit(s);			
		}
	}
	
	@SuppressWarnings("serial")
	private static class Sample implements Serializable {
	
		final List<Object> keySet;
		final Map<Object, Object> statics;
		final Object[] measureKeys;
		final Object[] measures;
		
		public Sample(List<Object> keySet, Map<Object, Object> statics, Object[] measureKeys, Object[] measures) {
			this.keySet = keySet;
			this.statics = statics;
			this.measureKeys = measureKeys;
			this.measures = measures;
		}
	}
	
}

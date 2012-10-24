package org.gridkit.nimble.metering;

import java.util.HashMap;
import java.util.Map;

import org.gridkit.nimble.statistics.TimeUtils;

public class PrintingSchema implements SampleSchema {
    private final Map<Object, Object> attrs;
    
    public PrintingSchema(Map<Object, Object> attrs) {
        this.attrs = attrs;
    }
    
    public PrintingSchema() {
        this(new HashMap<Object, Object>());
    }

    @Override
    public SampleSchema createDerivedScheme() {
        return new PrintingSchema(new HashMap<Object, Object>(attrs));
    }

    @Override
    public SampleFactory createFactory() {
        return new Factory(new HashMap<Object, Object>(attrs));
    }

    @Override
    public SampleSchema setStatic(Object key, Object value) {
        attrs.put(key, value);
        return this;
    }

    @Override
    public SampleSchema declareDynamic(Object key, Class<?> type) {
        return this;
    }
    
    @Override
	public void freeze() {
		// do nothing
	}

	public static class Factory implements SampleFactory {
        private final Map<Object, Object> attrs;
        
        public Factory(Map<Object, Object> attrs) {
            this.attrs = attrs;
        }
        
        @Override
        public SampleWriter newSample() {
            return new Writer(new HashMap<Object, Object>(attrs));
        }
    }
    
    public static class Writer implements SampleWriter {
        private Map<Object, Object> attrs;

        public Writer(Map<Object, Object> attrs) {
            this.attrs = attrs;
        }

        @Override
        public SampleWriter setMeasure(double measure) {
            attrs.put(Measure.MEASURE, measure);
            return this;
        }

        @Override
        public SampleWriter setTimestamp(long timestamp) {
            attrs.put(Measure.TIMESTAMP, TimeUtils.normalize(timestamp));
            return this;
        }
        
        @Override
		public SampleWriter setTimeBounds(long start, long finish) {
        	attrs.put(Measure.TIMESTAMP, TimeUtils.normalize(start));
        	attrs.put(Measure.END_TIMESTAMP, TimeUtils.normalize(finish));
			return this;
		}

		@Override
        public SampleWriter set(Object key, int value) {
            attrs.put(key, value);
            return this;
        }

        @Override
        public SampleWriter set(Object key, long value) {
            attrs.put(key, value);
            return this;
        }

        @Override
        public SampleWriter set(Object key, double value) {
            attrs.put(key, value);
            return this;
        }

        @Override
        public SampleWriter set(Object key, Object value) {
            attrs.put(key, value);
            return this;
        }

        @Override
        public SampleWriter set(Object key, String value) {
            attrs.put(key, value);
            return this;
        }

        @Override
        public void submit() {
            System.out.println(attrs);
        }
    }
}

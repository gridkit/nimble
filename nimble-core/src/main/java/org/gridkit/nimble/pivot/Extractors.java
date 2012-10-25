package org.gridkit.nimble.pivot;

import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleReader;

public class Extractors {
	
	public static SampleExtractor field(Object key) {
		return new FieldExtractor(key);
	}

	public static SampleExtractor constant(double value) {
		return new ConstExtractor(value);
	}

	public static SampleExtractor constant(Object value) {
		return new ConstExtractor(value);
	}
	
    public static SampleExtractor duration() {
        return new DurationExtractor();
    }
	
	public static class FieldExtractor implements SampleExtractor {

		private static final long serialVersionUID = 20121014L;
		
		private final Object key;
		
		public FieldExtractor(Object key) {
			this.key = key;
		}

		@Override
		public Object extract(SampleReader sample) {
			return sample.get(key);
		}
	}

	public static class ConstExtractor implements SampleExtractor {

		private static final long serialVersionUID = 20121014L;
		
		private final Object value;
		
		public ConstExtractor(Object value) {
			this.value = value;
		}

		@Override
		public Object extract(SampleReader sample) {
			return value;
		}
	}
	
	public static class DurationExtractor implements SampleExtractor {

        private static final long serialVersionUID = 3939066356481747214L;

        public DurationExtractor() {
        }

        @Override
        public Object extract(SampleReader sample) {
            double startTs = ((Number)sample.get(Measure.TIMESTAMP)).doubleValue();
            double finishTs = ((Number)sample.get(Measure.END_TIMESTAMP)).doubleValue();
                        
            return (finishTs - startTs);
        }
    }
}

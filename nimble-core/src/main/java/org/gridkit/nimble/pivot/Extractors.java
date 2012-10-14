package org.gridkit.nimble.pivot;

import org.gridkit.nimble.metering.SampleReader;

public class Extractors {
	
	public static Pivot.Extractor field(Object key) {
		return new FieldExtractor(key);
	}

	public static Pivot.Extractor constant(double value) {
		return new ConstExtractor(value);
	}
	
	
	public static class FieldExtractor implements Pivot.Extractor {

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

	public static class ConstExtractor implements Pivot.Extractor {

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
}

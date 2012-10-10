package org.gridkit.nimble.pivot;

import org.gridkit.nimble.metering.SampleReader;

public class Extractors {
	
	public static Pivot.Extractor field(Object key) {
		return new FieldExtractor(key);
	}
	
	public static class FieldExtractor implements Pivot.Extractor {

		private final Object key;
		
		public FieldExtractor(Object key) {
			this.key = key;
		}

		@Override
		public Object extract(SampleReader sample) {
			return sample.get(key);
		}
	}

}

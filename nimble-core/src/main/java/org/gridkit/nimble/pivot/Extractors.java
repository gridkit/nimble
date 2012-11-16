package org.gridkit.nimble.pivot;

import java.io.Serializable;

import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.pivot.CommonStats.StatAppraisal;
import org.gridkit.nimble.statistics.CombinedSummary;

public class Extractors {
	
	public static SampleExtractor field(Object key) {
		return new FieldExtractor(key);
	}

	public static SampleExtractor measure() {
		return new FieldExtractor(Measure.MEASURE);
	}

	public static SampleExtractor duartion() {
		return new FieldExtractor(Measure.DURATION);
	}

	public static SampleExtractor summary(Object key) {
		return new FieldExtractor(Measure.summary(key));
	}

	public static SampleExtractor stats(Object key, CommonStats.StatAppraisal app) {
		return new StaticExtractor(summary(key), app);
	}

	public static SampleExtractor constant(double value) {
		return new ConstExtractor(value);
	}

	public static SampleExtractor constant(Object value) {
		return new ConstExtractor(value);
	}
	
	public static class FieldExtractor implements SampleExtractor, Serializable {

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

	public static class StaticExtractor implements SampleExtractor, Serializable {
		
		private static final long serialVersionUID = 20121014L;
		
		private final SampleExtractor extractor;
		private final CommonStats.StatAppraisal app;
		
		public StaticExtractor(SampleExtractor extractor, StatAppraisal app) {
			this.extractor = extractor;
			this.app = app;
		}


		@Override
		public Object extract(SampleReader sample) {
			CombinedSummary summary = (CombinedSummary) extractor.extract(sample);			
			return summary == null ? null : app.extract(summary);
		}
	}

	public static class ConstExtractor implements SampleExtractor, Serializable {

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

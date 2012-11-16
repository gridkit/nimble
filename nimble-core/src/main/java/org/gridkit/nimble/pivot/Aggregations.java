package org.gridkit.nimble.pivot;

import java.io.Serializable;

public class Aggregations {

	public static Pivot.AggregationFactory createGaussianAggregator(SampleExtractor extractor) {
		return new DistributionAggregator(extractor);
	}
	
	public static Pivot.AggregationFactory createFrequencyAggregator(SampleExtractor extractor) {
		return new FrequencyAggregator(new SampleFrequencyExtractor(extractor));
	}

	public static Pivot.AggregationFactory createDistictAggregator(SampleExtractor extractor) {
		return new DistinctAggregator(extractor);
	}

	public static Pivot.AggregationFactory createConstantAggregator(final SampleExtractor extractor) {
		return new ConstantAggregator(extractor);
	}

	public static Pivot.AggregationFactory createStaticValue(final Object value) {
		return new StaticValue(value);
	}
		
	private static final class ConstantAggregator implements Pivot.AggregationFactory, Serializable {

		private static final long serialVersionUID = 20121014L;
		
		private final SampleExtractor extractor;

		private ConstantAggregator(SampleExtractor extractor) {
			this.extractor = extractor;
		}

		@Override
		public Aggregation<?> newAggregation() {
			return new ConstantAggregation(extractor);
		}
	}

	private static final class StaticValue implements Pivot.AggregationFactory, Serializable {

		private static final long serialVersionUID = 20121014L;
		
		private final Object value;

		public StaticValue(Object value) {
			this.value = value;
		}

		@Override
		public Aggregation<?> newAggregation() {
			return new StaticAggregation<Object>(value);
		}
	}
	
	private static class DistributionAggregator implements Pivot.AggregationFactory, Serializable {

		private static final long serialVersionUID = 20121010L;
		
		private final SampleExtractor extractor;
		
		public DistributionAggregator(SampleExtractor extractor) {
			this.extractor = extractor;
		}
		
		@Override
		public Aggregation<?> newAggregation() {
			return new DistributionAggregation(extractor);
		}		
	}
	
	private static class FrequencyAggregator implements Pivot.AggregationFactory, Serializable {
		
		private static final long serialVersionUID = 20121014L;
		
		private final EventFrequencyExtractor extractor;

		public FrequencyAggregator(EventFrequencyExtractor extractor) {
			this.extractor = extractor;
		}
		
		@Override
		public Aggregation<?> newAggregation() {
			return new FrequencyAggregation(extractor);
		}				
	}	

	private static class DistinctAggregator implements Pivot.AggregationFactory, Serializable {
		
		private static final long serialVersionUID = 20121014L;
		
		private final SampleExtractor extractor;
		
		public DistinctAggregator(SampleExtractor extractor) {
			this.extractor = extractor;
		}
		
		@Override
		public Aggregation<?> newAggregation() {
			return new DistinctAggregation(extractor);
		}				
	}	
}

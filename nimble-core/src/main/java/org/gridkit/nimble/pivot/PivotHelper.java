package org.gridkit.nimble.pivot;

import java.io.Serializable;

import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.pivot.Pivot.Extractor;

class PivotHelper {

	public static Pivot.Aggregator createGaussianAggregator(Pivot.Extractor extractor) {
		return new GaussianAggregator(extractor);
	}
	
	public static Pivot.Aggregator createDiscretHistogramAggregator(Extractor field) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Pivot.Aggregator createFrequencyAggregator(Extractor field) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Pivot.Aggregator createConstantAggregator(final Extractor extractor) {
		return new ConstantAggregator(extractor);
	}

	public static Pivot.Aggregator createStaticValue(final Object value) {
		return new StaticValue(value);
	}

	public static DisplayFunction displayField(Object key) {
		return new SimpleDisplayFunction(key.toString(), Extractors.field(key));
	}

	public static DisplayFunction displayDistributionStats(Object key) {
		return new DisplayDistributionFunction(Extractors.field(key), CommonStats.GENERIC_STATS);
	}

	public static DisplayFunction displayDistributionStats(Object key, CommonStats.StatAppraisal... params) {
		return new DisplayDistributionFunction(Extractors.field(key), params);
	}
		
	private static final class ConstantAggregator implements Pivot.Aggregator, Serializable {

		private final Extractor extractor;

		private ConstantAggregator(Extractor extractor) {
			this.extractor = extractor;
		}

		@Override
		public Aggregation<?> newAggregation() {
			return new ConstantAggregation(extractor);
		}
	}

	private static final class StaticValue implements Pivot.Aggregator, Serializable {
		
		private final Object value;

		public StaticValue(Object value) {
			this.value = value;
		}

		@Override
		public Aggregation<?> newAggregation() {
			return new StaticAggregation<Object>(value);
		}
	}
	
	private static class GaussianAggregator implements Pivot.Aggregator, Serializable {

		private static final long serialVersionUID = 20121010L;
		
		private final Pivot.Extractor extractor;
		
		public GaussianAggregator(Pivot.Extractor extractor) {
			this.extractor = extractor;
		}
		
		@Override
		public Aggregation<?> newAggregation() {
			return new DistributionAggregation(extractor);
		}
		
	}
	
	private static class SimpleDisplayFunction implements DisplayFunction {
		
		private final String caption;
		private final Pivot.Extractor extrator;
		
		public SimpleDisplayFunction(String caption, Extractor extrator) {
			this.caption = caption;
			this.extrator = extrator;
		}

		@Override
		public void getDisplayValue(CellPrinter printer, SampleReader level) {
			printer.addCell(caption, extrator.extract(level));
			
		}
	}
}

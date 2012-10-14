package org.gridkit.nimble.pivot;

import java.io.Serializable;

import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.pivot.Pivot.Extractor;
import org.gridkit.nimble.statistics.FrequencySummary;

class PivotHelper {

	public static Pivot.Aggregator createGaussianAggregator(Pivot.Extractor extractor) {
		return new DistributionAggregator(extractor);
	}
	
	public static Pivot.Aggregator createFrequencyAggregator(Extractor extractor) {
		return new FrequencyAggregator(new StandardEventFrequencyExtractor(extractor));
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
	
	public static DisplayFunction displayFrequency(Object key) {
		return new FrequencyDisplayFunction("Freq.", Extractors.field(key));
	}
		
	private static final class ConstantAggregator implements Pivot.Aggregator, Serializable {

		private static final long serialVersionUID = 20121014L;
		
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
	
	private static class DistributionAggregator implements Pivot.Aggregator, Serializable {

		private static final long serialVersionUID = 20121010L;
		
		private final Pivot.Extractor extractor;
		
		public DistributionAggregator(Pivot.Extractor extractor) {
			this.extractor = extractor;
		}
		
		@Override
		public Aggregation<?> newAggregation() {
			return new DistributionAggregation(extractor);
		}		
	}
	
	private static class FrequencyAggregator implements Pivot.Aggregator, Serializable {
		
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

	private static class FrequencyDisplayFunction implements DisplayFunction {
		
		private final String caption;
		private final Pivot.Extractor extrator;
		
		public FrequencyDisplayFunction(String caption, Extractor extrator) {
			this.caption = caption;
			this.extrator = extrator;
		}
		
		@Override
		public void getDisplayValue(CellPrinter printer, SampleReader level) {
			printer.addCell(caption, ((FrequencySummary)extrator.extract(level)).getWeigthedFrequency());			
		}
	}
}

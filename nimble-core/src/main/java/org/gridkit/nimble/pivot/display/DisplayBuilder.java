package org.gridkit.nimble.pivot.display;

import java.util.Arrays;
import java.util.List;

import org.gridkit.nimble.metering.DistributedMetering;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.pivot.Aggregations;
import org.gridkit.nimble.pivot.CommonStats;
import org.gridkit.nimble.pivot.CommonStats.StatAppraisal;
import org.gridkit.nimble.pivot.Extractors;
import org.gridkit.nimble.pivot.Pivot.AggregationFactory;
import org.gridkit.nimble.pivot.SampleExtractor;

public abstract class DisplayBuilder {
	
	public static ForLevelDisplayBuider with(PrintConfig pp) {
		return new ForLevelDisplayBuider(null, pp);
	}

	public static ForLevelDisplayBuider with(PrintConfig pp, String scope) {
		return new ForLevelDisplayBuider(scope, pp);
	}

	final PrintConfig printer;
	String globalScope;
	String scope;
	List<Object> deco;
	StatAppraisal subaggregate;
	
	protected DisplayBuilder(String globalScope, PrintConfig pp) {
		this.globalScope = globalScope;
		this.printer = pp;
	}
	
	protected void add(DisplayComponent dc) {
		if (deco != null) {
			dc = new DecorationAdapter(deco, dc);
		}
		if (globalScope == null && scope == null) {
			printer.add(dc);
		}
		else {
			String s = globalScope;
			if (s == null) {
				s = scope;
			}
			else if (scope != null) {
				s += "." + scope;
			}
			else {
				s += ".**";
			}
			
			printer.add(s, dc);
		}
	}
	
	public abstract ForLevelDisplayBuider constant(String caption, Object value);

	public abstract WithUnitsDisplayBuilder attribute(String caption, Object key);

	public abstract WithCaptionAndUnitsDisplayBuilder attribute(Object key);

	public WithCaptionAndUnitsDisplayBuilder frequencyStats(Object key) {
		return stats(key, CommonStats.FREQUENCY_STATS);
	}

	public WithCaptionAndUnitsDisplayBuilder frequencyStats() {
		return frequencyStats(Measure.MEASURE);
	}

	public WithCaptionAndUnitsDisplayBuilder distributionStats(Object key) {
		return stats(key, CommonStats.DISTRIBUTION_STATS);
	}

	public WithCaptionAndUnitsDisplayBuilder distributionStats() {
		return distributionStats(Measure.MEASURE);
	}
	
	public abstract WithCaptionAndUnitsDisplayBuilder stats(Object key, CommonStats.StatAppraisal... stats);

	public abstract WithCaptionDisplayBuilder value(SampleExtractor extractor);

	public WithCaptionAndUnitsDisplayBuilder count() {
		return stats(Measure.MEASURE, CommonStats.COUNT);
	}

	public WithCaptionAndUnitsDisplayBuilder count(Object key) {
		return stats(key, CommonStats.COUNT);
	}

	public WithCaptionAndUnitsDisplayBuilder mean() {
		return stats(Measure.MEASURE, CommonStats.MEAN);
	}

	public WithCaptionAndUnitsDisplayBuilder mean(Object key) {
		return stats(key, CommonStats.MEAN);
	}

	public WithCaptionAndUnitsDisplayBuilder stdDev() {
		return stats(Measure.MEASURE, CommonStats.STD_DEV);
	}

	public WithCaptionAndUnitsDisplayBuilder stdDev(Object key) {
		return stats(key, CommonStats.STD_DEV);
	}
	
	public WithCaptionAndUnitsDisplayBuilder min() {
		return stats(Measure.MEASURE, CommonStats.MIN);
	}

	public WithCaptionAndUnitsDisplayBuilder min(Object key) {
		return stats(key, CommonStats.MIN);
	}
	
	public WithCaptionAndUnitsDisplayBuilder max() {
		return stats(Measure.MEASURE, CommonStats.MAX);
	}

	public WithCaptionAndUnitsDisplayBuilder max(Object key) {
		return stats(key, CommonStats.MAX);
	}

	public WithCaptionAndUnitsDisplayBuilder sum() {
		return stats(Measure.MEASURE, CommonStats.SUM);
	}

	public WithCaptionAndUnitsDisplayBuilder sum(Object key) {
		return stats(key, CommonStats.SUM);
	}

	public WithCaptionAndUnitsDisplayBuilder frequency() {
		return stats(Measure.MEASURE, CommonStats.FREQUENCY);
	}

	public WithCaptionAndUnitsDisplayBuilder frequency(Object key) {
		return stats(key, CommonStats.EVENT_FREQUENCY);
	}

	public WithCaptionAndUnitsDisplayBuilder eventFrequency() {
		return stats(Measure.MEASURE, CommonStats.FREQUENCY);
	}
	
	public WithCaptionAndUnitsDisplayBuilder eventFrequency(Object key) {
		return stats(key, CommonStats.EVENT_FREQUENCY);
	}
	
	public WithCaptionAndUnitsDisplayBuilder duration() {
		return stats(Measure.MEASURE, CommonStats.DURATION);
	}

	public WithCaptionAndUnitsDisplayBuilder duration(Object key) {
		return stats(key, CommonStats.DURATION);
	}

	public WithCaptionAndUnitsDisplayBuilder distinct() {
		return stats(Measure.MEASURE, CommonStats.DISTINCT);
	}

	public WithCaptionAndUnitsDisplayBuilder distinct(Object key) {
		return stats(key, CommonStats.DISTINCT);
	}
	
	public abstract ForLevelDisplayBuider metricName(String caption);

	public abstract WithUnitsDisplayBuilder metricName();

	public abstract ForLevelDisplayBuider nodeName(String caption);

	public abstract WithUnitsDisplayBuilder nodeName();

	public abstract ForLevelDisplayBuider hostname(String caption);

	public abstract WithUnitsDisplayBuilder hostname();
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public CalcOver<SingleOnlyDisplayBuilder> calc() {
		return new CalcOver((SingleOnlyDisplayBuilder) this);
	}

	public static abstract class DecoDisplayBuilder extends DisplayBuilder {

		public DecoDisplayBuilder(String globalScope, PrintConfig pp) {
			super(globalScope, pp);
		}

		public DisplayBuilder deco(String... deco) {
			return decorated(deco);
		}
		
		@Deprecated
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public DisplayBuilder decorated(String... deco) {
			this.deco = (List)Arrays.asList(deco);
			return this;
		}
	}
	
	public static class ForLevelDisplayBuider extends DecoDisplayBuilder implements SingleOnlyDisplayBuilder {

		public ForLevelDisplayBuider(String globalScope, PrintConfig pp) {
			super(globalScope, pp);
		}
		
		public DecoDisplayBuilder level(String pattern) {
			scope = pattern;
			return this;
		}

		@Override
		public ForLevelDisplayBuider constant(String caption, Object value) {
			DisplayComponent dc = DisplayFactory.constant(caption, value);
			add(dc);
			return this;
		}

		@Override
		public WithCaptionAndUnitsDisplayBuilder attribute(Object key) {
			SimpleDisplayComponent dc = DisplayFactory.attribute(key);
			add(dc);
			return new WithCaptionAndUnitsDisplayBuilder(globalScope, printer, dc);
		}

		@Override
		public WithUnitsDisplayBuilder attribute(String caption, Object key) {
			SimpleDisplayComponent dc = DisplayFactory.attribute(caption, key);
			add(dc);
			return new WithCaptionAndUnitsDisplayBuilder(globalScope, printer, dc);
		}
		
		@Override
		public WithCaptionAndUnitsDisplayBuilder hostname() {
			return (WithCaptionAndUnitsDisplayBuilder) attribute("Hostname", DistributedMetering.HOSTNAME);
		}

		@Override
		public ForLevelDisplayBuider hostname(String caption) {
			return attribute(caption, DistributedMetering.HOSTNAME);
		}
		
		@Override
		public WithCaptionAndUnitsDisplayBuilder nodeName() {
			return (WithCaptionAndUnitsDisplayBuilder) attribute("Node", DistributedMetering.NODENAME);
		}

		@Override
		public ForLevelDisplayBuider nodeName(String caption) {
			return attribute(caption, DistributedMetering.NODENAME);
		}

		@Override
		public WithCaptionAndUnitsDisplayBuilder metricName() {
			return (WithCaptionAndUnitsDisplayBuilder) attribute("Name", Measure.NAME);
		}

		@Override
		public ForLevelDisplayBuider metricName(String caption) {
			return attribute(caption, Measure.NAME);
		}

		@Override
		public WithCaptionAndUnitsDisplayBuilder stats(Object key, CommonStats.StatAppraisal... stats) {
			StatsDisplayComponent ds;
			if (subaggregate != null) {
				if (stats.length > 1) {
					throw new IllegalArgumentException("Could aggregate only over one measure");
				}
				AggregationFactory aggFactory = createAggregator(Extractors.stats(key, stats[0]), subaggregate);
				SampleExtractor calc = new SubAggreagtor(aggFactory);
				ds = new StatsDisplayComponent(calc, subaggregate);
				subaggregate = null;
			}
			else {
				ds = DisplayFactory.genericStats(key, stats);
			}
			add(ds);
			return new WithCaptionAndUnitsDisplayBuilder(globalScope, printer, ds);
		}
		
		public WithCaptionDisplayBuilder value(SampleExtractor extractor) {
			SimpleDisplayComponent sd = new SimpleDisplayComponent(extractor.toString(), extractor);
			add(sd);
			return new WithCaptionDisplayBuilder(globalScope, printer, sd);
		}
	}
	
	public static class WithUnitsDisplayBuilder extends ForLevelDisplayBuider {
		
		final DisplayConfigurable component;

		WithUnitsDisplayBuilder(String globalScope, PrintConfig pp, DisplayConfigurable component) {
			super(globalScope, pp);
			this.component = component;
		}
		
		public ForLevelDisplayBuider as(UnitDeco deco) {
			component.setUnits(deco);
			return this;
		}
		
		public ForLevelDisplayBuider asMillis() {
			return as(Units.MILLIS);
		}		

		public ForLevelDisplayBuider asMiB() {
			return as(Units.MiB);
		}		

		public ForLevelDisplayBuider asPercent() {
			return as(Units.PERCENT);
		}		
	}

	public static class WithCaptionAndUnitsDisplayBuilder extends WithUnitsDisplayBuilder {

		public WithCaptionAndUnitsDisplayBuilder(String globalScope, PrintConfig pp, DisplayConfigurable component) {
			super(globalScope, pp, component);
		}
		
		public WithUnitsDisplayBuilder caption(String caption) {
			component.setCaption(caption);
			return this;
		}
		
	}
	
	public static class WithCaptionDisplayBuilder extends ForLevelDisplayBuider {
		
		private final DisplayConfigurable component;
		
		WithCaptionDisplayBuilder(String globalScope, PrintConfig pp, DisplayConfigurable component) {
			super(globalScope, pp);
			this.component = component;
		}
		
		public DisplayBuilder caption(String caption) {
			component.setCaption(caption);
			return this;
		}
	}
	
	public static class CalcOver<T> {
		
		private final T next;

		public CalcOver(T next) {
			this.next = next;
		}
		
		public T stat(CommonStats.StatAppraisal app) {
			((DisplayBuilder)next).subaggregate = app;
			return next;
		}
		
		public T count() {
			return stat(CommonStats.COUNT);
		}

		public T distinct() {
			return stat(CommonStats.DISTINCT);
		}

		public T duration() {
			return stat(CommonStats.DURATION);
		}
		
		public T frequency() {
			return stat(CommonStats.FREQUENCY);
		}
		
		public T max() {
			return stat(CommonStats.MAX);
		}
		
		public T min() {
			return stat(CommonStats.MIN);
		}
		
		public T mean() {
			return stat(CommonStats.MEAN);
		}
		
		public T stdDev() {
			return stat(CommonStats.STD_DEV);
		}		
		
		public T sum() {
			return stat(CommonStats.SUM);
		}		
		
		public T variance() {
			return stat(CommonStats.VARIANCE);
		}		
	}
	
	public static interface SingleOnlyDisplayBuilder {
		
		public WithCaptionAndUnitsDisplayBuilder count();

		public WithCaptionAndUnitsDisplayBuilder count(Object key);

		public WithCaptionAndUnitsDisplayBuilder mean();

		public WithCaptionAndUnitsDisplayBuilder mean(Object key);

		public WithCaptionAndUnitsDisplayBuilder stdDev();

		public WithCaptionAndUnitsDisplayBuilder stdDev(Object key);
		
		public WithCaptionAndUnitsDisplayBuilder min();

		public WithCaptionAndUnitsDisplayBuilder min(Object key);
		
		public WithCaptionAndUnitsDisplayBuilder max();

		public WithCaptionAndUnitsDisplayBuilder max(Object key);

		public WithCaptionAndUnitsDisplayBuilder sum();

		public WithCaptionAndUnitsDisplayBuilder sum(Object key);

		public WithCaptionAndUnitsDisplayBuilder frequency();

		public WithCaptionAndUnitsDisplayBuilder frequency(Object key);

		public WithCaptionAndUnitsDisplayBuilder duration();

		public WithCaptionAndUnitsDisplayBuilder duration(Object key);

		public WithCaptionAndUnitsDisplayBuilder distinct();

		public WithCaptionAndUnitsDisplayBuilder distinct(Object key);
	}
	
	private static AggregationFactory createAggregator(SampleExtractor extractor, CommonStats.StatAppraisal app) {
		switch (app) {
		case COUNT:
		case MAX:
		case MEAN:
		case MIN:
		case STD_DEV:
		case SUM:
		case VARIANCE:
			return Aggregations.createGaussianAggregator(extractor);
		case DURATION:
		case FREQUENCY:
			return Aggregations.createFrequencyAggregator(extractor);
		case DISTINCT:
			return Aggregations.createDistictAggregator(extractor);
		}
		throw new IllegalArgumentException("Unknown: " + app);
	}
}

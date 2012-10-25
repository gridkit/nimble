package org.gridkit.nimble.pivot.display;

import java.util.Arrays;
import java.util.List;

import org.gridkit.nimble.metering.DisrtibutedMetering;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.pivot.CommonStats;

public abstract class DisplayBuilder {
	
	public static ForLevelDisplayBuider with(PivotPrinter2 pp) {
		return new ForLevelDisplayBuider(null, pp);
	}

	public static ForLevelDisplayBuider with(PivotPrinter2 pp, String scope) {
		return new ForLevelDisplayBuider(scope, pp);
	}

	final PivotPrinter2 printer;
	String globalScope;
	String scope;
	List<Object> deco;
	
	protected DisplayBuilder(String globalScope, PivotPrinter2 pp) {
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

	public abstract WithCaptionAndUnitsDisplayBuilder frequencyStats(Object key);

	public WithCaptionAndUnitsDisplayBuilder frequencyStats() {
		return frequencyStats(Measure.MEASURE);
	}

	public abstract WithCaptionAndUnitsDisplayBuilder distributionStats(Object key);

	public WithCaptionAndUnitsDisplayBuilder distributionStats() {
		return distributionStats(Measure.MEASURE);
	}
	
	public abstract WithCaptionAndUnitsDisplayBuilder stats(Object key, CommonStats.StatAppraisal... stats);

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
		return stats(key, CommonStats.FREQUENCY);
	}

	public WithCaptionAndUnitsDisplayBuilder duration() {
		return stats(Measure.MEASURE, CommonStats.DURATION);
	}

	public WithCaptionAndUnitsDisplayBuilder duration(Object key) {
		return stats(key, CommonStats.DURATION);
	}
	
	public abstract ForLevelDisplayBuider metricName(String caption);

	public abstract WithUnitsDisplayBuilder metricName();

	public abstract ForLevelDisplayBuider nodeName(String caption);

	public abstract WithUnitsDisplayBuilder nodeName();

	public abstract ForLevelDisplayBuider hostname(String caption);

	public abstract WithUnitsDisplayBuilder hostname();
	
	public static abstract class DecoDisplayBuilder extends DisplayBuilder {

		public DecoDisplayBuilder(String globalScope, PivotPrinter2 pp) {
			super(globalScope, pp);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public DisplayBuilder decorated(String... deco) {
			this.deco = (List)Arrays.asList(deco);
			return this;
		}
	}
	
	public static class ForLevelDisplayBuider extends DecoDisplayBuilder {

		public ForLevelDisplayBuider(String globalScope, PivotPrinter2 pp) {
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
			return (WithCaptionAndUnitsDisplayBuilder) attribute("Hostname", DisrtibutedMetering.HOSTNAME);
		}

		@Override
		public ForLevelDisplayBuider hostname(String caption) {
			return attribute(caption, DisrtibutedMetering.HOSTNAME);
		}
		
		@Override
		public WithCaptionAndUnitsDisplayBuilder nodeName() {
			return (WithCaptionAndUnitsDisplayBuilder) attribute("Node", DisrtibutedMetering.NODENAME);
		}

		@Override
		public ForLevelDisplayBuider nodeName(String caption) {
			return attribute(caption, DisrtibutedMetering.NODENAME);
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
			StatsDisplayComponent ds = DisplayFactory.genericStats(key, stats);
			add(ds);
			return new WithCaptionAndUnitsDisplayBuilder(globalScope, printer, ds);
		}

		@Override
		public WithCaptionAndUnitsDisplayBuilder distributionStats(Object key) {
			return stats(key, CommonStats.DISTRIBUTION_STATS);
		}

		@Override
		public WithCaptionAndUnitsDisplayBuilder frequencyStats(Object key) {
			return stats(key, CommonStats.FREQUENCY_STATS);
		}
	}
	
	public static class WithUnitsDisplayBuilder extends ForLevelDisplayBuider {
		
		final DisplayConfigurable component;

		WithUnitsDisplayBuilder(String globalScope, PivotPrinter2 pp, DisplayConfigurable component) {
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
	}

	public static class WithCaptionAndUnitsDisplayBuilder extends WithUnitsDisplayBuilder {

		public WithCaptionAndUnitsDisplayBuilder(String globalScope, PivotPrinter2 pp, DisplayConfigurable component) {
			super(globalScope, pp, component);
		}
		
		public WithUnitsDisplayBuilder caption(String caption) {
			component.setCaption(caption);
			return this;
		}
		
	}
	
	public static class WithCaptionDisplayBuilder extends ForLevelDisplayBuider {
		
		private final DisplayConfigurable component;
		
		WithCaptionDisplayBuilder(String globalScope, PivotPrinter2 pp, DisplayConfigurable component) {
			super(globalScope, pp);
			this.component = component;
		}
		
		public DisplayBuilder caption(String caption) {
			component.setCaption(caption);
			return this;
		}
	}
}

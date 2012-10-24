package org.gridkit.nimble.pivot.display;

import java.util.Arrays;
import java.util.List;

import org.gridkit.nimble.metering.DisrtibutedMetering;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.pivot.CommonStats;

public abstract class DisplayBuilder {
	
	public static DisplayBuilder with(PivotPrinter2 pp) {
		return new ForLevelDisplayBuider(pp);
	}

	final PivotPrinter2 printer;
	String scope;
	String decoCaption = "%s";
	List<Object> deco;
	
	protected DisplayBuilder(PivotPrinter2 pp) {
		this.printer = pp;
	}
	
	protected void add(DisplayComponent dc) {
		if (deco != null) {
			dc = new DecorationAdapter(decoCaption, deco, dc);
		}
		if (scope == null) {
			printer.add(dc);
		}
		else {
			printer.add(scope, dc);
		}
	}
	
	public abstract ForLevelDisplayBuider constant(String caption, Object value);

	public abstract WithUnitsDisplayBuilder attribute(String caption, Object key);

	public abstract WithUnitsDisplayBuilder attribute(Object key);

	public abstract WithUnitsDisplayBuilder frequencyStats(Object key);

	public WithUnitsDisplayBuilder frequencyStats() {
		return frequencyStats(Measure.MEASURE);
	}

	public abstract WithUnitsDisplayBuilder distributionStats(Object key);

	public WithUnitsDisplayBuilder distributionStats() {
		return distributionStats(Measure.MEASURE);
	}
	
	public abstract WithUnitsDisplayBuilder stats(String caption, Object key, CommonStats.StatAppraisal... stats);

	public abstract WithUnitsDisplayBuilder stats(Object key, CommonStats.StatAppraisal... stats);

	public abstract ForLevelDisplayBuider measureName(String caption);

	public abstract ForLevelDisplayBuider measureName();

	public abstract ForLevelDisplayBuider nodename(String caption);

	public abstract ForLevelDisplayBuider nodename();

	public abstract ForLevelDisplayBuider hostname(String caption);

	public abstract ForLevelDisplayBuider hostname();
	
	public static abstract class DecoDisplayBuilder extends DisplayBuilder {

		public DecoDisplayBuilder(PivotPrinter2 pp) {
			super(pp);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public DisplayBuilder decorated(String caption, String... deco) {
			this.decoCaption = caption;
			this.deco = (List)Arrays.asList(deco);
			return this;
		}
	}
	
	public static class ForLevelDisplayBuider extends DecoDisplayBuilder {

		public ForLevelDisplayBuider(PivotPrinter2 pp) {
			super(pp);
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
		public WithUnitsDisplayBuilder attribute(Object key) {
			SimpleDisplayComponent dc = DisplayFactory.attribute(key);
			add(dc);
			return new WithUnitsDisplayBuilder(printer, dc);
		}

		@Override
		public WithUnitsDisplayBuilder attribute(String caption, Object key) {
			SimpleDisplayComponent dc = DisplayFactory.attribute(caption, key);
			add(dc);
			return new WithUnitsDisplayBuilder(printer, dc);
		}
		
		@Override
		public ForLevelDisplayBuider hostname() {
			return attribute("Hostname", DisrtibutedMetering.HOSTNAME);
		}

		@Override
		public ForLevelDisplayBuider hostname(String caption) {
			return attribute(caption, DisrtibutedMetering.HOSTNAME);
		}
		
		@Override
		public ForLevelDisplayBuider nodename() {
			return attribute("Node", DisrtibutedMetering.NODENAME);
		}

		@Override
		public ForLevelDisplayBuider nodename(String caption) {
			return attribute(caption, DisrtibutedMetering.NODENAME);
		}

		@Override
		public ForLevelDisplayBuider measureName() {
			return attribute("Name", Measure.NAME);
		}

		@Override
		public ForLevelDisplayBuider measureName(String caption) {
			return attribute(caption, Measure.NAME);
		}

		@Override
		public WithUnitsDisplayBuilder stats(Object key, CommonStats.StatAppraisal... stats) {
			StatsDisplayComponent ds = DisplayFactory.genericStats(key, stats);
			add(ds);
			return new WithUnitsDisplayBuilder(printer, ds);
		}

		@Override
		public WithUnitsDisplayBuilder stats(String caption, Object key, CommonStats.StatAppraisal... stats) {
			StatsDisplayComponent ds = DisplayFactory.genericStats(caption, key, stats);
			add(ds);
			return new WithUnitsDisplayBuilder(printer, ds);
		}
		
		@Override
		public WithUnitsDisplayBuilder distributionStats(Object key) {
			return stats(key, CommonStats.DISTRIBUTION_STATS);
		}

		@Override
		public WithUnitsDisplayBuilder frequencyStats(Object key) {
			return stats(key, CommonStats.FREQUENCY_STATS);
		}
	}
	
	public static class WithUnitsDisplayBuilder extends ForLevelDisplayBuider {
		
		private final WithUnits component;

		WithUnitsDisplayBuilder(PivotPrinter2 pp, WithUnits component) {
			super(pp);
			this.component = component;
		}
		
		public DisplayBuilder as(UnitDeco deco) {
			component.setUnits(deco);
			return this;
		}
		
		public DisplayBuilder asMillis() {
			return as(Units.MILLIS);
		}		
	}
}

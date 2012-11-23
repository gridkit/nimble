package org.gridkit.nimble.monitoring;

import java.util.EnumSet;

import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SamplerBuilder;
import org.gridkit.nimble.orchestration.ScenarioBuilder;
import org.gridkit.nimble.orchestration.TimeLine;
import org.gridkit.nimble.pivot.Extractors;
import org.gridkit.nimble.pivot.Filters;
import org.gridkit.nimble.pivot.Pivot;
import org.gridkit.nimble.pivot.Pivot.Level;
import org.gridkit.nimble.pivot.display.DisplayBuilder;
import org.gridkit.nimble.pivot.display.PrintConfig;

public class StandardSamplerReportBundle extends AbstractMonitoringBundle {

	public static final ReportType DISTRIBUTION = ReportType.DISTRIBUTION;
	public static final ReportType TIME_DISTRIBUTION = ReportType.TIME_DISTRIBUTION;
	public static final ReportType FREQUENCY = ReportType.FREQUENCY;
	public static final ReportType WEIGHTED_FREQUENCY = ReportType.WEIGHTED_FREQUENCY;
	
	public enum ReportType { DISTRIBUTION, TIME_DISTRIBUTION, FREQUENCY, WEIGHTED_FREQUENCY }; 
	
	private String domain;
	private EnumSet<ReportType> reports = EnumSet.allOf(ReportType.class);
	
	public StandardSamplerReportBundle(String domain) {
		super(domain);
		this.domain = domain;
	}

	public StandardSamplerReportBundle(String domain, String namespace) {
		super(namespace);
		this.domain = domain;
	}

	@Override
	public String getDescription() {
		return "User sampler report";
	}

	public void showValueDistribution(boolean show) {
		if (show) {
			reports.add(DISTRIBUTION);
		}
		else {
			reports.remove(DISTRIBUTION);
		}
	}

	public void showTimeDistribution(boolean show) {
		if (show) {
			reports.add(TIME_DISTRIBUTION);
		}
		else {
			reports.remove(TIME_DISTRIBUTION);
		}
	}

	public void showEventFrequency(boolean show) {
		if (show) {
			reports.add(FREQUENCY);
		}
		else {
			reports.remove(FREQUENCY);
		}
	}

	public void showWeightedFrequency(boolean show) {
		if (show) {
			reports.add(WEIGHTED_FREQUENCY);
		}
		else {
			reports.remove(WEIGHTED_FREQUENCY);
		}
	}
	
	@Override
	public void configurePivot(Pivot pivot) {
		Level base = pivot.root()
			.level(namespace)
				.filter(Measure.DOMAIN, domain)
				.filter(Measure.PRODUCER, SamplerBuilder.Producer.USER)
					.group(SamplerBuilder.OPERATION);
		
		for(Object g: groupping) {
			base = base.group(g);
		}

		Level summary = base.level("");
		addAggregators(summary);
		summary.calcConstant(Measure.NAME, Extractors.field(SamplerBuilder.OPERATION));
		
		Level breakdown = base.level("")
				.filter(Filters.notNull(SamplerBuilder.DESCRIMINATOR))
					.group(SamplerBuilder.DESCRIMINATOR).level("");
		breakdown.calcConstant(Measure.NAME, Extractors.field(Measure.NAME));
		
		addAggregators(breakdown);		
	}

	protected void addAggregators(Level level) {
		level
			.calcDistribution(Measure.DURATION)
			.calcDistribution(Measure.MEASURE);
		
		if (reports.contains(ReportType.FREQUENCY)) {
			level.calcFrequency(1, 1);
		}
		if (reports.contains(ReportType.WEIGHTED_FREQUENCY)) {
			level.calcFrequency(Measure.MEASURE);
		}
	}

	@Override
	public void configurePrinter(PrintConfig printer) {
		printConfig.replay(printer);
		
		DisplayBuilder db = DisplayBuilder.with(printer, namespace);
		db
			.attribute("Metric", Measure.NAME)
			.count();
		if (reports.contains(DISTRIBUTION)) {
			db.distributionStats(Measure.MEASURE).caption("%s");
			db.sum(Measure.MEASURE).caption("%s");
		}
		if (reports.contains(TIME_DISTRIBUTION)) {
			db.distributionStats(Measure.DURATION).caption("Dur. %s [ms]").asMillis();
		}
		if (reports.contains(FREQUENCY)) {
			db.frequency(1).caption("Freq. [Op/S]");
		}
		if (reports.contains(WEIGHTED_FREQUENCY)) {
			db.frequency(Measure.MEASURE).caption("W.Freq. [1/S]");
			if (!reports.contains(DISTRIBUTION)) {
				db.sum(Measure.MEASURE).caption("%s");
			}
		}
		if (reports.contains(FREQUENCY) || reports.contains(WEIGHTED_FREQUENCY)) {
			db.duration().caption("Observed [S]");
		}
	}

	@Override
	public void deploy(ScenarioBuilder sb, ServiceProvider context, TimeLine timeLine) {
		// do nothing
	}
}

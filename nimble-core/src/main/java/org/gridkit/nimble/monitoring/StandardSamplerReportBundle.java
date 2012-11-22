package org.gridkit.nimble.monitoring;

import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SamplerBuilder;
import org.gridkit.nimble.orchestration.ScenarioBuilder;
import org.gridkit.nimble.orchestration.TimeLine;
import org.gridkit.nimble.pivot.Pivot;
import org.gridkit.nimble.pivot.display.DisplayBuilder;
import org.gridkit.nimble.pivot.display.PrintConfig;

public class StandardSamplerReportBundle extends AbstractMonitoringBundle {

	public StandardSamplerReportBundle(String namespace) {
		super(namespace);
	}

	@Override
	public String getDescription() {
		return "User sampler report";
	}

	@Override
	public void configurePivot(Pivot pivot) {
		pivot.root()
			.level(namespace)
				.filter(Measure.PRODUCER, SamplerBuilder.Producer.USER)
					.group(Measure.NAME)
						.calcDistribution(Measure.DURATION)
						.calcFrequency(Measure.MEASURE);
		
	}

	@Override
	public void configurePrinter(PrintConfig printer) {
		printConfig.replay(printer);
		
		DisplayBuilder.with(printer, namespace)
		.attribute("Metric", Measure.NAME)
		.count()
		.distributionStats(Measure.DURATION).caption("%s [ms]").asMillis()
		.frequency()
		.duration().caption("Observed [S]");		
	}

	@Override
	public void deploy(ScenarioBuilder sb, ServiceProvider context, TimeLine timeLine) {
		// do nothing
	}
}

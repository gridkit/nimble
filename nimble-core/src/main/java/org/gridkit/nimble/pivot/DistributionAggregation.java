package org.gridkit.nimble.pivot;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.statistics.DistributionSummary;
import org.gridkit.nimble.statistics.StatsOps;

public class DistributionAggregation implements Aggregation<DistributionSummary> {

	private final SampleExtractor extractor;
	private DistributionSummary baseStats;
	private SummaryStatistics runningStats;
	
	public DistributionAggregation(SampleExtractor extractor) {
		this.extractor = extractor;
	}
	
	@Override
	public void addSamples(SampleReader reader) {
		add(extractor.extract(reader));
	}

	private void add(Object extract) {
		if (extract instanceof Number) {
			if (runningStats == null) {
				runningStats = new SummaryStatistics();
			}
			double v = ((Number)extract).doubleValue();
			runningStats.addValue(v);
		}
		else if (extract instanceof DistributionSummary) {
			DistributionSummary ss = (DistributionSummary) extract;
			merge(ss);
		}
	}

	@Override
	public void addAggregate(DistributionSummary aggregate) {
		merge(aggregate);
	}

	@Override
	public DistributionSummary getResult() {
		merge(null);
		return baseStats == null ? StatsOps.EMPTY_DISTRIBUTION_SUMMARY : baseStats;
	}
	
	private void merge(DistributionSummary aggregate) {
		if (baseStats != null && runningStats != null) {
			baseStats = StatsOps.combine(baseStats, new DistributionSummary.Values(runningStats));
			runningStats = null;
		}
		if (runningStats != null) {
			baseStats = new DistributionSummary.Values(runningStats);
		}		
		if (baseStats != null && aggregate != null) {
			baseStats = StatsOps.combine(baseStats, aggregate);
		}
		else if (aggregate != null){
			baseStats = aggregate;
		}
		
	}
}

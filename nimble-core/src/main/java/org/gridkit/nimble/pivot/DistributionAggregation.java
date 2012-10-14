package org.gridkit.nimble.pivot;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.statistics.StatsOps;

public class DistributionAggregation implements Aggregation<StatisticalSummary> {

	private final Pivot.Extractor extractor;
	private StatisticalSummary baseStats;
	private SummaryStatistics runningStats;
	
	public DistributionAggregation(Pivot.Extractor extractor) {
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
		else if (extract instanceof StatisticalSummary) {
			StatisticalSummary ss = (StatisticalSummary) extract;
			merge(ss);
		}
	}

	@Override
	public void addAggregate(StatisticalSummary aggregate) {
		merge(aggregate);
	}

	@Override
	public StatisticalSummary getResult() {
		merge(null);
		return baseStats == null ? emptyStats() : baseStats;
	}
	
	private void merge(StatisticalSummary aggregate) {
		if (baseStats != null && runningStats != null) {
			baseStats = StatsOps.combine(baseStats, runningStats);
			runningStats = null;
		}
		if (runningStats != null) {
			baseStats = runningStats;
		}		
		if (baseStats != null && aggregate != null) {
			baseStats = StatsOps.combine(baseStats, aggregate);
		}
		else if (aggregate != null){
			baseStats = aggregate;
		}
		
	}

	private StatisticalSummary emptyStats() {
		StatisticalSummaryValues value = new StatisticalSummaryValues(Double.NaN, Double.NaN, 0, Double.NaN, Double.NaN, Double.NaN);
		return value;
	}
}

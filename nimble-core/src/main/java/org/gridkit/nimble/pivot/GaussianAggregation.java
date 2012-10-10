package org.gridkit.nimble.pivot;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.statistics.StatsOps;

public class GaussianAggregation implements Aggregation<StatisticalSummary> {

	private final Pivot.Extractor extractor;
	private StatisticalSummary baseStats;
	private SummaryStatistics runningStats;
	
	public GaussianAggregation(Pivot.Extractor extractor) {
		this.extractor = extractor;
	}
	
	@Override
	public void addSamples(SampleReader reader) {
		add(extractor.extract(reader));
	}

	private void add(Object extract) {
		if (runningStats == null) {
			runningStats = new SummaryStatistics();
		}
		double v = ((Number)extract).doubleValue();
		runningStats.addValue(v);
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
		if (baseStats != null && aggregate != null) {
			baseStats = StatsOps.combine(baseStats, aggregate);
		}
		if (baseStats != null && runningStats != null) {
			baseStats = StatsOps.combine(baseStats, runningStats);
			runningStats = null;
		}
		if (runningStats != null) {
			baseStats = runningStats;
		}		
	}

	private StatisticalSummary emptyStats() {
		StatisticalSummaryValues value = new StatisticalSummaryValues(Double.NaN, Double.NaN, 0, Double.NaN, Double.NaN, Double.NaN);
		return value;
	}
}

package org.gridkit.nimble.statistics.simple;

import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.gridkit.nimble.statistics.DelegatingStatisticalSummary;
import org.gridkit.nimble.statistics.StatsOps;
import org.gridkit.nimble.statistics.ThroughputSummary;

public class SimpleThroughputSummary extends DelegatingStatisticalSummary implements ThroughputSummary {    
    private double durationMs;
    
    public SimpleThroughputSummary(StatisticalSummary valStats, double scale, double startMs, double finishMs) {
        this(valStats, scale, Math.max((finishMs - startMs), 1));
    }
    
    public SimpleThroughputSummary(StatisticalSummary valStats, double scale, double durationMs) {
        super(StatsOps.scale(valStats, scale));
        this.durationMs = durationMs;
    }

    @Override
    public double getThroughput(TimeUnit timeUnit) {
        return getSum() / getDuration(timeUnit);
    }

    @Override
    public double getDuration(TimeUnit timeUnit) {
        return durationMs * StatsOps.getScale(TimeUnit.MILLISECONDS, timeUnit);
    }
}

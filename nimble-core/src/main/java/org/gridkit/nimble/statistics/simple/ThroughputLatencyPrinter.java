package org.gridkit.nimble.statistics.simple;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.gridkit.nimble.print.LinePrinter.Contetx;
import org.gridkit.nimble.statistics.StatsOps;
import org.gridkit.nimble.statistics.ThroughputSummary;

public class ThroughputLatencyPrinter extends AbstractSimpleStatsLinePrinter {
    private static final Map<TimeUnit, String> timeAliasMap = new HashMap<TimeUnit, String>();

    static {
        timeAliasMap.put(TimeUnit.NANOSECONDS,  "ns");
        timeAliasMap.put(TimeUnit.MICROSECONDS, "us");
        timeAliasMap.put(TimeUnit.MILLISECONDS, "ms");
        timeAliasMap.put(TimeUnit.SECONDS,      "s");
        timeAliasMap.put(TimeUnit.HOURS,        "h");
        timeAliasMap.put(TimeUnit.DAYS,         "d");
    }
    
    @Override
    protected String getSamplerName() {
        return ThroughputLatencyReporter.SAMPLER_NAME;
    }

    @Override
    protected void print(Map<String, StatisticalSummary> aggregates, Contetx context) {
        StatisticalSummary ltStats = getLatency(aggregates, TimeUnit.MILLISECONDS);
        ThroughputSummary thStats = getThroughput(aggregates, 1.0);
        
        StatisticalSummary genStats = (ltStats != null ? ltStats : thStats);

        if (genStats != null) {
            context.cell("N", genStats.getN());
          
            if (ltStats != null) {
                context.cell("Mean (ms)", ltStats.getMean());
                context.cell("Sd (ms^2)", ltStats.getStandardDeviation());
                context.cell("Min (ms)",  ltStats.getMin());
                context.cell("Max (ms)",  ltStats.getMax());
            } else {
                context.cell("Mean (ms)", "");
                context.cell("Sd (ms^2)", "");
                context.cell("Min (ms)",  "");
                context.cell("Max (ms)",  "");
            }
            
            if (thStats != null) {
                context.cell("Th (osp/s)", thStats.getThroughput(TimeUnit.SECONDS));
                context.cell("Dur (s)", thStats.getDuration(TimeUnit.SECONDS));
            } else {
                context.cell("Th (osp/s)", "");
                context.cell("Dur (s)", "");
            }
            
            context.newline();
        }
    }
    
    public static StatisticalSummary getLatency(String statistica, SimpleStats stats, TimeUnit timeUnit) {
        Map<String, StatisticalSummary> unmarked = SimpleStatsOps.unmark(stats.toMap()).get(ThroughputLatencyReporter.SAMPLER_NAME);
        unmarked = SimpleStatsOps.unmark(unmarked).get(statistica);
        return getLatency(unmarked, timeUnit);
    }
    
    private static StatisticalSummary getLatency(Map<String, StatisticalSummary> aggregates, TimeUnit timeUnit) {
        StatisticalSummary vs = aggregates.get(ThroughputLatencyReporter.TIME_NS);
        
        if (vs == null) {
            return null;
        } else {
            return StatsOps.scale(vs, StatsOps.getScale(TimeUnit.NANOSECONDS, timeUnit));
        }
    }
    
    private static ThroughputSummary getThroughput(Map<String, StatisticalSummary> aggregates, double scale) {
        if (!isThroughput(aggregates)) {
            return null;
        }
        
        StatisticalSummary opsAggr    = aggregates.get(ThroughputLatencyReporter.OPS);;
        StatisticalSummary startAggr  = aggregates.get(ThroughputLatencyReporter.START_MS);
        StatisticalSummary finishAggr = aggregates.get(ThroughputLatencyReporter.FINISH_MS);
        
        return new SimpleThroughputSummary(opsAggr, scale, startAggr.getMin(), finishAggr.getMax());
    }
    
    private static boolean isThroughput(Map<String, StatisticalSummary> aggregates) {
        return aggregates.containsKey(ThroughputLatencyReporter.START_MS) &&
               aggregates.containsKey(ThroughputLatencyReporter.FINISH_MS) && 
               aggregates.containsKey(ThroughputLatencyReporter.OPS);
    }
}

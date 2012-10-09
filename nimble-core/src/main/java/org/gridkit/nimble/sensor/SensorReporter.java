package org.gridkit.nimble.sensor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.gridkit.nimble.statistics.DelegatingStatsReporter;
import org.gridkit.nimble.statistics.StatsOps;
import org.gridkit.nimble.statistics.StatsReporter;
import org.gridkit.nimble.statistics.ThroughputSummary;
import org.gridkit.nimble.statistics.simple.SimpleStats;
import org.gridkit.nimble.statistics.simple.SimpleThroughputSummary;

@SuppressWarnings("serial")
public class SensorReporter extends DelegatingStatsReporter implements Serializable {
    public static String TIME_MS_MARK = "time_ms";
    public static String VALUE_MARK = "value";
    
    public SensorReporter(StatsReporter delegate) {
        super(delegate);
    }
    
    public void report(String statistica, double value, long time, TimeUnit unit) {
        double timeMs = StatsOps.convert(time, unit, TimeUnit.MILLISECONDS);
        
        Map<String, Object> sample = new HashMap<String, Object>();
        
        sample.put(StatsOps.mark(statistica, TIME_MS_MARK), timeMs);
        sample.put(StatsOps.mark(statistica, VALUE_MARK), value);
        
        getDelegate().report(sample);
    }
    
    public void report(String statistica, double value, long timeNs) {
        report(statistica, value, timeNs, TimeUnit.NANOSECONDS);
    }
    
    public static ThroughputSummary getThroughput(String statistica, SimpleStats stats, double scale) {
        StatisticalSummary timeMs = stats.getValStats(StatsOps.mark(statistica, TIME_MS_MARK));
        StatisticalSummary value = stats.getValStats(StatsOps.mark(statistica, VALUE_MARK));
        
        return new SimpleThroughputSummary(value, scale, timeMs.getSum());
    }
    
    public static void getThroughput(String statistica, SimpleStats stats) {
        getThroughput(statistica, stats, 1.0d);
    }
}

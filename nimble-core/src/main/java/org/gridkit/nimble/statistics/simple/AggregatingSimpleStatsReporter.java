package org.gridkit.nimble.statistics.simple;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.gridkit.nimble.platform.SystemTimeService;
import org.gridkit.nimble.platform.TimeService;
import org.gridkit.nimble.statistics.StatsReporter;

@SuppressWarnings("serial")
public class AggregatingSimpleStatsReporter implements StatsReporter, Serializable {
    private final SimpleStatsAggregator aggr;
    private final long aggrPeriodNs;

    private transient TimeService timeSrv;
    private transient Long lastAggrNs;
    private transient SimpleStatsProducer statsProd;
    
    public AggregatingSimpleStatsReporter(SimpleStatsAggregator aggr, long aggrPeriod, TimeUnit unit) {
        this.aggr = aggr;
        this.aggrPeriodNs = unit.toNanos(aggrPeriod);
        this.init();
    }
    
    public AggregatingSimpleStatsReporter(SimpleStatsAggregator aggr, long aggrPeriodS) {
        this(aggr, aggrPeriodS, TimeUnit.SECONDS);
    }

    @Override
    public void report(Map<String, Object> sample) {
        long timeNs = timeSrv.currentTimeNanos();
        
        if (lastAggrNs == null) {
            lastAggrNs = timeNs;
        }
        
        statsProd.report(sample);
        
        if (timeNs - lastAggrNs > aggrPeriodNs) {
            SimpleStats stats = statsProd.produce();
            aggr.add(stats);
            
            lastAggrNs = timeSrv.currentTimeNanos();
            statsProd = new SimpleStatsProducer();
        }
    }

    private void init() {
        this.timeSrv = SystemTimeService.getInstance();
        this.lastAggrNs = null;
        this.statsProd = new SimpleStatsProducer();
    }
    
    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        init();
    }
}

package org.gridkit.nimble.statistics.simple;

import static org.gridkit.nimble.statistics.simple.SimpleStatsOps.mark;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.gridkit.nimble.platform.TimeService;
import org.gridkit.nimble.statistics.DelegatingStatsReporter;
import org.gridkit.nimble.statistics.StatsOps;
import org.gridkit.nimble.statistics.StatsReporter;

@SuppressWarnings("serial")
public class ThroughputLatencyReporter extends DelegatingStatsReporter {
    public static final String SAMPLER_NAME = "th";
    
    public static final String START_MS  = "start_ms";
    public static final String FINISH_MS = "finish_ms";
    public static final String TIME_NS   = "time_ns";
    public static final String OPS       = "ops";

    private static final String START_NS = "start_ns";
    
    private final TimeService timeService;

    private final Map<String, Map<String, Object>> attrsMap;
    
    public ThroughputLatencyReporter(StatsReporter delegate, TimeService timeService) {
        super(delegate);
        this.timeService = timeService;
        this.attrsMap = new HashMap<String, Map<String, Object>>();
    }

    public void start(String statistica) {
        Map<String, Object> attrs = getAttrs(statistica);
        
        attrs.put(START_MS, timeService.currentTimeMillis());
        attrs.put(START_NS, timeService.currentTimeNanos());
    }
    
    public void operations(String statistica, int count) {
        getAttrs(statistica).put(OPS, count);
    }
    
    public void finish(String statistica) {
        try {
            long finishNs = timeService.currentTimeNanos();
            long finishMs = timeService.currentTimeMillis();

            Map<String, Object> attrs = getAttrs(statistica);
            
            Long startMs = (Long)attrs.get(START_MS);
            Long startNs = (Long)attrs.get(START_NS);

            if (startNs != null && startMs != null) {
                if (!attrs.containsKey(OPS)) {
                    attrs.put(OPS, 1);
                }
                
                attrs.put(FINISH_MS, finishMs);
                attrs.put(TIME_NS, finishNs - startNs);
                attrs.remove(START_NS);
                
                report(statistica, attrs);
            } else if (startNs != null || startMs != null) {
                throw new IllegalStateException("startTimeNanos and startTimeMillis are unsync");
            }
        } finally {
            removeAttrs(statistica);
        }
    }
    
    public void report(String statistica, Map<String, Object> attrs) {
        Map<String, Object> report = new HashMap<String, Object>();
        
        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            report.put(mark(SAMPLER_NAME, statistica, entry.getKey()), entry.getValue());
        }
        
        getDelegate().report(report);
    }
    
    public void latency(String statistica, double latency, TimeUnit unit) {
        Map<String, Object> report = new HashMap<String, Object>();
        
        report.put(mark(SAMPLER_NAME, statistica, TIME_NS), StatsOps.convert(latency, unit, TimeUnit.NANOSECONDS));
        
        getDelegate().report(report);
    }
    
    public void report(String attr, Object value) {
        getDelegate().report(Collections.singletonMap(attr, value));
    }
    
    private Map<String, Object> getAttrs(String statistica) {
        Map<String, Object> statAttrs = attrsMap.get(statistica);
        
        if (statAttrs == null) {
            statAttrs = new HashMap<String, Object>();
            attrsMap.put(statistica, statAttrs);
        }
        
        return statAttrs;
    }
    
    private void removeAttrs(String statistica) {
        attrsMap.remove(statistica);
    }
}

package org.gridkit.nimble.statistics.simple;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.gridkit.nimble.statistics.StatsOps;

@SuppressWarnings("serial")
public class SimpleStats implements Serializable {        
    private final Map<String, StatisticalSummary> valStats;

    public SimpleStats() {
        this(new HashMap<String, StatisticalSummary>());
    }
    
    public SimpleStats(Map<String, StatisticalSummary> statsMap) {
        this.valStats = statsMap;
    }
    
    public StatisticalSummary getValStats(String name) {
        return valStats.get(name);
    }
    
    public StatisticalSummary getValStats(String statistica, String mark) {
        return valStats.get(StatsOps.mark(statistica, mark));
    }
    
    public Set<String> getValStatsNames() {
        return new HashSet<String>(valStats.keySet());
    }
    
    public Map<String, StatisticalSummary> toMap() {
        return new HashMap<String, StatisticalSummary>(valStats);
    }
    
    public static SimpleStats combine(SimpleStats s1, SimpleStats s2) {
        return new SimpleStats(combine(s1.valStats, s2.valStats));
    }
    
    private static Map<String, StatisticalSummary> combine(Map<String, StatisticalSummary> m1, Map<String, StatisticalSummary> m2) {
        Set<String> statsNames = new HashSet<String>(m1.keySet());
        statsNames.addAll(m2.keySet());
        
        Map<String, StatisticalSummary> result = new HashMap<String, StatisticalSummary>();
        
        for (String statsName : statsNames) {
            result.put(statsName, combine(m1.get(statsName), m2.get(statsName)));
        }
        
        return result;
    }
    
    private static StatisticalSummary combine(StatisticalSummary s1, StatisticalSummary s2) {
        if (s1 == null) {
            return s2;
        } else if (s2 == null) {
            return s1;
        } else {
            return StatsOps.combine(s1, s2);
        }
    }
}

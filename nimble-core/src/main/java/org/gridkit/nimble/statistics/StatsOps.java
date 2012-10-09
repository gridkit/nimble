package org.gridkit.nimble.statistics;

import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.gridkit.nimble.util.Pair;
import org.gridkit.nimble.util.ValidOps;

public class StatsOps {
    public static final String MARK_SEP = "^";
    
    private static final StatisticalSummary emptySummary = (new SummaryStatistics()).getSummary();
    
    public static StatisticalSummary combine(StatisticalSummary s1, StatisticalSummary s2) {
        if (s1.getN() == 0){
            return s2;
        } else if (s2.getN() == 0) {
            return s1;
        } else if (s1.getN() == 0 && s2.getN() == 0) {
            return emptySummary;
        }
        
        long n = s1.getN() + s2.getN();
        
        double mean = (s1.getN() * s1.getMean() + s2.getN() * s2.getMean()) / n;
        
        double s1Diff = (mean - s1.getMean()) * (mean - s1.getMean());
        double s2Diff = (mean - s2.getMean()) * (mean - s2.getMean());
        
        double var = (s1.getN() * (s1.getVariance() + s1Diff) + s2.getN() * (s2.getVariance() + s2Diff)) / n;
        
        double sum = s1.getSum() + s2.getSum();
        
        double max = Math.max(s1.getMax(), s2.getMax());
        double min = Math.min(s1.getMin(), s2.getMin());
        
        return new StatisticalSummaryValues(mean, var, n, max, min, sum);
    }
    
    public static StatisticalSummary scale(StatisticalSummary s, double scale) {
        double square = scale * scale;
        
        return new StatisticalSummaryValues(
            s.getMean() * scale, s.getVariance() * square, s.getN(), s.getMax() * scale, s.getMin() * scale, s.getSum() * scale
        );
    }
    
    public static double getScale(TimeUnit from, TimeUnit to) {
        double fromInNs = from.toNanos(1);
        double toInNs = to.toNanos(1);
        
        return fromInNs / toInNs;
    }
    
    public static double convert(double time, TimeUnit from, TimeUnit to) {
        return time * getScale(from, to);
    }
    
    public static String mark(String statistica, String mark) {
        ValidOps.notEmpty(statistica, "statistica");
        ValidOps.notEmpty(statistica, "mark");
        
        if (statistica.contains(MARK_SEP)) {
            throw new IllegalArgumentException("statistica");
        }
        
        return statistica + MARK_SEP + mark;
    }
    
    public static Pair<String, String> unmark(String str) {
        int index = str.indexOf(MARK_SEP);
        int lastIndex = str.lastIndexOf(MARK_SEP);
        
        if (index == -1 || index != lastIndex || index == str.length() - 1 || index == 0) {
            throw new IllegalArgumentException("str");
        }
        
        return Pair.newPair(str.substring(0, index), str.substring(index + 1));
    }

}

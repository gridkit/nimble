package org.gridkit.nimble.statistics.simple;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;

public class SimpleStatsOps {
    public static final String MARK_SEP       = "^";
    public static final String MARK_SEP_REGEX = "\\^";
    
    public static String mark(String... strs) {
        return mark(Arrays.asList(strs));
    }
    
    public static String mark(List<String> strs) {
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < strs.size(); ++i) {
            result.append(strs.get(i));
            
            if (i != strs.size() - 1) {
                result.append(MARK_SEP);
            }
        }
        
        return result.toString();
    }
    
    public static List<String> unmark(String str) {
        return Arrays.asList(str.split(MARK_SEP_REGEX));
    }
    
    public static Map<String, Map<String, StatisticalSummary>> unmark(Map<String, StatisticalSummary> aggregates) {        
        Map<String, Map<String, StatisticalSummary>> result = new TreeMap<String, Map<String, StatisticalSummary>>();
        
        for (Map.Entry<String, StatisticalSummary> entry : aggregates.entrySet()) {
            List<String> marks = unmark(entry.getKey());
            
            Map<String, StatisticalSummary> subMarks = result.get(marks.get(0));
            
            if (subMarks == null) {
                subMarks = new HashMap<String, StatisticalSummary>();
                result.put(marks.get(0), subMarks);
            }
            
            if (marks.size() == 1) {
                subMarks.put("", entry.getValue());
            } else {
                subMarks.put(mark(marks.subList(1, marks.size())), entry.getValue());
            }
        }
                
        return result;
    }
}

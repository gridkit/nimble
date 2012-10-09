package org.gridkit.nimble.statistics.simple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.gridkit.nimble.print.LinePrinter;
import org.gridkit.nimble.print.LinePrinter.Contetx;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

public abstract class AbstractSimpleStatsLinePrinter implements SimpleStatsTablePrinter.SimpleStatsLinePrinter {
    protected static final String VALUE = "Value";
    
    private Predicate<String> valuePredicate = Predicates.alwaysTrue();
    
    protected abstract String getSamplerName();
        
    protected abstract void print(Map<String, StatisticalSummary> aggregates, LinePrinter.Contetx context);
    
    @Override
    public void print(SimpleStats stats, LinePrinter.Contetx context) {
        Map<String, Map<String, StatisticalSummary>> values = filter(stats);
        
        for (Map.Entry<String, Map<String, StatisticalSummary>> entry : values.entrySet()) {
            String value = entry.getKey();
            
            if (valuePredicate.apply(value)) {
                print(entry.getValue(), new InternalLinePrinterContext(value, context));
            }
        }
    }
    
    private static class InternalLinePrinterContext implements LinePrinter.Contetx {
        private String value;
        private LinePrinter.Contetx context;
        private boolean firstPrint = true;

        public InternalLinePrinterContext(String value, Contetx context) {
            this.value = value;
            this.context = context;
        }

        @Override
        public void newline() {
            if (firstPrint) {
                context.cell(VALUE, value);
            }
            
            context.newline();
            
            firstPrint = true;
        }

        @Override
        public void cell(String name, Object object) {
            if (firstPrint) {
                context.cell(VALUE, value);
                firstPrint = false;
            }
            
            context.cell(name, object);
        }
    }
    
    private Map<String, Map<String, StatisticalSummary>> filter(SimpleStats stats) {
        Map<String, Map<String, StatisticalSummary>> result = new TreeMap<String, Map<String,StatisticalSummary>>();
        
        for (String statistica : stats.getValStatsNames()) {
            List<String> unmarks = SimpleStatsOps.unmark(statistica);
            
            if (unmarks.size() > 2) {
                String sampler   = unmarks.get(0);
                String value     = unmarks.get(1);
                String aggregate = SimpleStatsOps.mark(unmarks.subList(2, unmarks.size()));
                
                if (sampler.equals(getSamplerName())) {                    
                    Map<String, StatisticalSummary> aggrs = result.get(value);
                    
                    if (aggrs == null) {
                        aggrs = new HashMap<String, StatisticalSummary>();
                        result.put(value, aggrs);
                    }
                    
                    aggrs.put(aggregate, stats.getValStats(statistica));
                }
            }
        }
        
        return result;
    }

    public Predicate<String> getValuePredicate() {
        return valuePredicate;
    }

    public void setValuePredicate(Predicate<String> valuePredicate) {
        this.valuePredicate = valuePredicate;
    }
}

package org.gridkit.nimble.statistics.simple;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.gridkit.nimble.print.LinePrinter.Context;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

public class WhitelistPrinter implements SimpleStatsTablePrinter.SimpleStatsLinePrinter {
    private Predicate<String> valuePredicate = Predicates.alwaysTrue();
    
    @Override
    public void print(SimpleStats stats, Context context) {
        for (String valStats : stats.getValStatsNames()) {
            if (valuePredicate.apply(valStats)) {
                StatisticalSummary aggr = stats.getValStats(valStats);
                
                context.cell(AbstractSimpleStatsLinePrinter.VALUE, valStats);
                context.cell("N",    aggr.getN());
                context.cell("Mean", aggr.getMean());
                context.cell("Sd",   aggr.getStandardDeviation());
                context.cell("Var",  aggr.getVariance());
                context.cell("Min",  aggr.getMin());
                context.cell("Max",  aggr.getMax());
                
                context.newline();
            }
        }
    }

    public Predicate<String> getValuePredicate() {
        return valuePredicate;
    }

    public void setValuePredicate(Predicate<String> valuePredicate) {
        this.valuePredicate = valuePredicate;
    }
}

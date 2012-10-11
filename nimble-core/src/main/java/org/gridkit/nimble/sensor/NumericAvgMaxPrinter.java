package org.gridkit.nimble.sensor;

import java.util.Map;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.gridkit.nimble.print.LinePrinter.Context;
import org.gridkit.nimble.statistics.simple.AbstractSimpleStatsLinePrinter;

public class NumericAvgMaxPrinter extends AbstractSimpleStatsLinePrinter {
    @Override
    protected String getSamplerName() {
        return "avgmax";
    }

    @Override
    protected void print(Map<String, StatisticalSummary> aggregates, Context context) {
    	StatisticalSummary avg = aggregates.get("avg");
        StatisticalSummary max = aggregates.get("max");

        if (avg != null) {
        	context.cell("Mean", avg.getMean());
        }

        if (max != null) {
        	context.cell("Max", max.getMax());
        }

        if (max != null) {
        	context.cell("# of Measures, Mean", max.getN());
        }

        if (avg != null) {
        	context.cell("# of Measures, Max", avg.getN());
        }
        
        context.newline();
    }
}

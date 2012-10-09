package org.gridkit.nimble.sensor;

import java.util.Map;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.gridkit.nimble.print.LinePrinter.Contetx;
import org.gridkit.nimble.statistics.simple.AbstractSimpleStatsLinePrinter;

public class NumericAveragePrinter extends AbstractSimpleStatsLinePrinter {
    @Override
    protected String getSamplerName() {
        return "avgmax";
    }

    @Override
    protected void print(Map<String, StatisticalSummary> aggregates, Contetx context) {
        StatisticalSummary avg = aggregates.get("avg");

        if (avg != null) {
        	context.cell("MBean.Avg", avg.getMean());
        	context.cell("# of Measures", avg.getN());
        }
        
        context.newline();
    }
}

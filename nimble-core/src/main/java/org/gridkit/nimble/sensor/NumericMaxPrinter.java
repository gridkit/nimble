package org.gridkit.nimble.sensor;

import java.util.Map;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.gridkit.nimble.print.LinePrinter.Contetx;
import org.gridkit.nimble.statistics.simple.AbstractSimpleStatsLinePrinter;

public class NumericMaxPrinter extends AbstractSimpleStatsLinePrinter {
    @Override
    protected String getSamplerName() {
        return "avgmax";
    }

    @Override
    protected void print(Map<String, StatisticalSummary> aggregates, Contetx context) {
        StatisticalSummary max = aggregates.get("max");

        if (max != null) {
        	context.cell("MBean.Max", max.getMax());
        	context.cell("# of Measures", max.getN());
        }
        
        context.newline();
    }
}

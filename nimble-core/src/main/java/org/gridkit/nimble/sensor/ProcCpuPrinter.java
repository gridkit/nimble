package org.gridkit.nimble.sensor;

import java.util.Map;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.gridkit.nimble.print.LinePrinter.Contetx;
import org.gridkit.nimble.statistics.simple.AbstractSimpleStatsLinePrinter;

public class ProcCpuPrinter extends AbstractSimpleStatsLinePrinter {
    @Override
    protected String getSamplerName() {
        return ProcCpuReporter.SAMPLER_NAME;
    }

    @Override
    protected void print(Map<String, StatisticalSummary> aggregates, Contetx context) {
        StatisticalSummary usr = aggregates.get(ProcCpuReporter.USR);
        StatisticalSummary sys = aggregates.get(ProcCpuReporter.SYS);
        StatisticalSummary tot = aggregates.get(ProcCpuReporter.TOT);
        
        StatisticalSummary time = aggregates.get(ProcCpuReporter.MS);
        
        StatisticalSummary cnt = aggregates.get(ProcCpuReporter.CNT);
        
        if (time != null) {
            context.cell("Usr", usr.getSum() / time.getSum() * cnt.getMean());
            context.cell("Sys", sys.getSum() / time.getSum() * cnt.getMean());
            context.cell("Tot", tot.getSum() / time.getSum() * cnt.getMean());
            
            context.cell("Usr/pid", usr.getSum() / time.getSum());
            context.cell("Sys/pid", sys.getSum() / time.getSum());
            context.cell("Tot/pid", tot.getSum() / time.getSum());
        } else {
            context.cell("Usr", "");
            context.cell("Sys", "");
            context.cell("Tot", "");
            
            context.cell("Usr/pid", "");
            context.cell("Sys/pid", "");
            context.cell("Tot/pid", "");
        }
        
        context.cell("# of Proc", cnt.getMean());
        
        if (time != null) {
            context.cell("# of Mesures", time.getN());
        } else {
            context.cell("# of Mesures", "");
        }
        
        context.newline();
    }
}

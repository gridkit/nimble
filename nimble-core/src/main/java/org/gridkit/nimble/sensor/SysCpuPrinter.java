package org.gridkit.nimble.sensor;

import java.util.Map;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.gridkit.nimble.print.LinePrinter.Context;
import org.gridkit.nimble.statistics.simple.AbstractSimpleStatsLinePrinter;

public class SysCpuPrinter extends AbstractSimpleStatsLinePrinter {
    @Override
    protected String getSamplerName() {
        return SysCpuReporter.SAMPLER_NAME;
    }

    @Override
    protected void print(Map<String, StatisticalSummary> aggregates, Context context) {
        StatisticalSummary usr     = aggregates.get(SysCpuReporter.USR);
        StatisticalSummary sys     = aggregates.get(SysCpuReporter.SYS);
        StatisticalSummary nice    = aggregates.get(SysCpuReporter.NICE);
        StatisticalSummary idle    = aggregates.get(SysCpuReporter.IDLE);
        StatisticalSummary wait    = aggregates.get(SysCpuReporter.WAIT);
        StatisticalSummary irq     = aggregates.get(SysCpuReporter.IRQ);
        StatisticalSummary softirq = aggregates.get(SysCpuReporter.SOFTIRQ);
        StatisticalSummary stolen  = aggregates.get(SysCpuReporter.STOLEN);
        StatisticalSummary tot     = aggregates.get(SysCpuReporter.TOT);
        
        StatisticalSummary time = aggregates.get(SysCpuReporter.MS);

        context.cell("Usr",     usr.getSum()     / time.getSum());
        context.cell("Sys",     sys.getSum()     / time.getSum());
        context.cell("Nice",    nice.getSum()    / time.getSum());
        context.cell("Idle",    idle.getSum()    / time.getSum());
        context.cell("Wait",    wait.getSum()    / time.getSum());
        context.cell("Irq",     irq.getSum()     / time.getSum());
        context.cell("SoftIrq", softirq.getSum() / time.getSum());
        context.cell("Stolen",  stolen.getSum()  / time.getSum());
        context.cell("Tot",     tot.getSum()     / time.getSum());
        
        context.cell("# of Mesures", time.getN());
        
        context.newline();
    }
}

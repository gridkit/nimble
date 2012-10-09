package org.gridkit.nimble.sensor;

import static org.gridkit.nimble.statistics.simple.SimpleStatsOps.unmark;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.gridkit.nimble.print.LinePrinter.Contetx;
import org.gridkit.nimble.statistics.StatsOps;
import org.gridkit.nimble.statistics.simple.AbstractSimpleStatsLinePrinter;

public class NetInterfacePrinter extends AbstractSimpleStatsLinePrinter {
    @Override
    protected String getSamplerName() {
        return NetInterfaceReporter.SAMPLER_NAME;
    }

    @Override
    protected void print(Map<String, StatisticalSummary> aggregates, Contetx context) {
        for (Map.Entry<String, Map<String, StatisticalSummary>> entry : unmark(aggregates).entrySet()) {
            context.cell("Interface", entry.getKey());
            
            double sentMb = entry.getValue().get(NetInterfaceReporter.SENT_BYTES).getSum() / 1024 / 1024;
            double receivedMb = entry.getValue().get(NetInterfaceReporter.RECEIVED_BYTES).getSum() / 1024/ 1024;
            
            StatisticalSummary timeMs = entry.getValue().get(NetInterfaceReporter.MS);
            
            double timeS = StatsOps.convert(timeMs.getSum(), TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
            
            context.cell("Sent Th (MB/s)", sentMb / timeS);
            context.cell("Received Th (MB/s)", receivedMb / timeS);
            context.cell("Total sent (MB)", sentMb);
            context.cell("Total Received (MB)", receivedMb);
            context.cell("Dur (s)", timeS);
            context.cell("# of Mesures", timeMs.getN());

            context.newline();
        }
    }
}

package org.gridkit.nimble.sensor;

import static org.gridkit.nimble.statistics.simple.SimpleStatsOps.mark;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.gridkit.nimble.statistics.StatsOps;
import org.gridkit.nimble.statistics.StatsReporter;
import org.hyperic.sigar.ProcTime;

@SuppressWarnings("serial")
public class ProcCpuReporter implements Sensor.Reporter<List<ProcCpuSensor.ProcCpuMeasure>>, Serializable {
    public static final String SAMPLER_NAME = "pcpu";
    
    public static String USR = "usr";
    public static String SYS = "sys";
    public static String TOT = "tot";
    public static String MS  = "ms";
    public static String CNT = "cnt";
    
    private String statistica;
    private StatsReporter statsReporter;

    public ProcCpuReporter(String statistica, StatsReporter statsReporter) {
        this.statistica = statistica;
        this.statsReporter = statsReporter;
    }

    @Override
    public void report(List<ProcCpuSensor.ProcCpuMeasure> measures) {
        for (ProcCpuSensor.ProcCpuMeasure measure : measures) {
            ProcTime leftCpu = measure.getLeftState();
            ProcTime rightCpu = measure.getRightState();
            
            Map<String, Object> sample = new HashMap<String, Object>();
            
            sample.put(mark(SAMPLER_NAME, statistica, USR), rightCpu.getUser()  - leftCpu.getUser());
            sample.put(mark(SAMPLER_NAME, statistica, SYS), rightCpu.getSys()   - leftCpu.getSys());
            sample.put(mark(SAMPLER_NAME, statistica, TOT), rightCpu.getTotal() - leftCpu.getTotal());
            
            sample.put(
                mark(SAMPLER_NAME, statistica, MS),
                StatsOps.convert(measure.getRightTsNs() - measure.getLeftTsNs(), TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS)
            );
            
            statsReporter.report(sample);
        }

        statsReporter.report(Collections.<String, Object>singletonMap(
            mark(SAMPLER_NAME, statistica, CNT), measures.size())
        );
    }
}

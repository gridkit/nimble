package org.gridkit.nimble.sensor;

import static org.gridkit.nimble.statistics.simple.SimpleStatsOps.mark;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.gridkit.nimble.statistics.StatsOps;
import org.gridkit.nimble.statistics.StatsReporter;
import org.hyperic.sigar.Cpu;

@SuppressWarnings("serial")
public class SysCpuReporter implements Sensor.Reporter<IntervalMeasure<Cpu>>, Serializable {
    public static final String SAMPLER_NAME = "scpu";
    
    public static String USR     = "usr";
    public static String SYS     = "sys";
    public static String NICE    = "nice";
    public static String IDLE    = "idle";
    public static String WAIT    = "wait";
    public static String IRQ     = "irq";
    public static String SOFTIRQ = "softIrq";
    public static String STOLEN  = "stolen";
    public static String TOT     = "tot";
    public static String MS      = "ms";

    private String statistica;
    private StatsReporter statsReporter;

    public SysCpuReporter(String statistica, StatsReporter statsReporter) {
        this.statistica = statistica;
        this.statsReporter = statsReporter;
    }
    
    @Override
    public void report(IntervalMeasure<Cpu> m) {
        if (m != null) {
            Cpu leftCpu = m.getLeftState();
            Cpu rightCpu = m.getRightState();
            
            Map<String, Object> sample = new HashMap<String, Object>();
            
            sample.put(mark(SAMPLER_NAME, statistica, USR),     rightCpu.getUser()    - leftCpu.getUser());
            sample.put(mark(SAMPLER_NAME, statistica, SYS),     rightCpu.getSys()     - leftCpu.getSys());
            sample.put(mark(SAMPLER_NAME, statistica, NICE),    rightCpu.getNice()    - leftCpu.getNice());
            sample.put(mark(SAMPLER_NAME, statistica, IDLE),    rightCpu.getIdle()    - leftCpu.getIdle());
            sample.put(mark(SAMPLER_NAME, statistica, WAIT),    rightCpu.getWait()    - leftCpu.getWait());
            sample.put(mark(SAMPLER_NAME, statistica, IRQ),     rightCpu.getIrq()     - leftCpu.getIrq());
            sample.put(mark(SAMPLER_NAME, statistica, SOFTIRQ), rightCpu.getSoftIrq() - leftCpu.getSoftIrq());
            sample.put(mark(SAMPLER_NAME, statistica, STOLEN),  rightCpu.getStolen()  - leftCpu.getStolen());
            sample.put(mark(SAMPLER_NAME, statistica, TOT),     rightCpu.getTotal()   - leftCpu.getTotal());
            
            double timeMs = StatsOps.convert(m.getRightTsNs() - m.getLeftTsNs(), TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS);
            
            sample.put(mark(SAMPLER_NAME, statistica, MS), timeMs);
                        
            statsReporter.report(sample);

        }
    }
}

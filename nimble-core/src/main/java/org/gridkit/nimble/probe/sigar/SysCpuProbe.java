package org.gridkit.nimble.probe.sigar;

import static org.gridkit.nimble.util.StringOps.F;

import java.util.concurrent.Callable;

import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.probe.RateSampler;
import org.gridkit.nimble.util.SafeCallable;
import org.hyperic.sigar.Cpu;

public class SysCpuProbe extends SigarHolder implements Callable<Void> {
    public static final String PROBE_TYPE = "sysCpu";
    
    private final RateSampler sampler;
    
    public SysCpuProbe(SampleSchema schema) {
        this.sampler = new RateSampler(schema, SigarMeasure.MEASURE_NAME_KEY, SigarMeasure.PROBE_TYPE_KEY, PROBE_TYPE);
    }

    public static Runnable newInstance(SampleSchema schema) {
        return new SafeCallable<Void>(new SysCpuProbe(schema));
    }
    
    @Override
    public Void call() throws Exception {
        long timestamp = System.currentTimeMillis();
        
        Cpu cpu = getSigar().getCpu();
        
        sampler.sample(SigarMeasure.CPU_USER,    cpu.getUser(),    timestamp);
        sampler.sample(SigarMeasure.CPU_SYSTEM,  cpu.getSys(),     timestamp);
        sampler.sample(SigarMeasure.CPU_NICE,    cpu.getNice(),    timestamp);
        sampler.sample(SigarMeasure.CPU_IDLE,    cpu.getIdle(),    timestamp);
        sampler.sample(SigarMeasure.CPU_WAIT,    cpu.getWait(),    timestamp);
        sampler.sample(SigarMeasure.CPU_IRQ,     cpu.getIrq(),     timestamp);
        sampler.sample(SigarMeasure.CPU_SOFTIRQ, cpu.getSoftIrq(), timestamp);
        sampler.sample(SigarMeasure.CPU_STOLEN,  cpu.getStolen(),  timestamp);
        sampler.sample(SigarMeasure.CPU_TOTAL,   cpu.getTotal(),   timestamp);

        return null;
    }

    @Override
    public String toString() {
        return F(SysCpuProbe.class.getSimpleName());
    }
}

package org.gridkit.nimble.probe.sigar;

import static org.gridkit.nimble.util.StringOps.F;

import java.util.concurrent.Callable;

import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.probe.MeasureSampler;
import org.gridkit.nimble.util.SafeCallable;
import org.hyperic.sigar.Mem;

public class SysMemProbe extends SigarHolder implements Callable<Void> {
    public static final String PROBE_TYPE = "sysMem";
    
    private final MeasureSampler sampler;
    
    public SysMemProbe(SampleSchema schema) {
        this.sampler = new MeasureSampler(schema, SigarMeasure.MEASURE_NAME, SigarMeasure.PROBE_TYPE, PROBE_TYPE);
    }

    public static Runnable newInstance(SampleSchema schema) {
        return new SafeCallable<Void>(new SysMemProbe(schema));
    }
    
    @Override
    public Void call() throws Exception {
        long timestamp = System.currentTimeMillis();
        
        Mem mem = getSigar().getMem();
        
        sampler.sample(SigarMeasure.MEM_RAM,         mem.getRam(),        timestamp);
        sampler.sample(SigarMeasure.MEM_USED,        mem.getUsed(),       timestamp);
        sampler.sample(SigarMeasure.MEM_FREE,        mem.getFree(),       timestamp);
        sampler.sample(SigarMeasure.MEM_ACTUAL_USED, mem.getActualUsed(), timestamp);
        sampler.sample(SigarMeasure.MEM_ACTUAL_FREE, mem.getActualFree(), timestamp);
        sampler.sample(SigarMeasure.MEM_TOTAL,       mem.getTotal(),      timestamp);
        
        return null;
    }

    @Override
    public String toString() {
        return F(SysMemProbe.class.getSimpleName());
    }
}

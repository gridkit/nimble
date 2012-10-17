package org.gridkit.nimble.probe.sigar;

import static org.gridkit.nimble.util.StringOps.F;

import java.util.concurrent.Callable;

import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.probe.MeasureSampler;
import org.gridkit.nimble.probe.ProbeMeasure;
import org.gridkit.nimble.probe.ProbeOps.SingleProbeFactory;
import org.gridkit.nimble.util.SafeCallable;
import org.hyperic.sigar.ProcMem;

public class ProcMemProbe extends SigarHolder implements Callable<Void> {
    public static final String PROBE_TYPE = "procMem";
    
    private final long pid;
    private final MeasureSampler sampler;
    
    public ProcMemProbe(long pid, SampleSchema schema) {
        this.pid = pid;
        this.sampler = new MeasureSampler(schema, SigarMeasure.PROBE_TYPE, PROBE_TYPE);
    }
        
    @Override
    public Void call() throws Exception {
        long timestamp = System.currentTimeMillis();
        
        ProcMem procMem = getSigar().getProcMem(pid);
                
        sampler.sample(SigarMeasure.MEM_SIZE,        procMem.getSize(),        timestamp);
        sampler.sample(SigarMeasure.MEM_RESIDENT,    procMem.getResident(),    timestamp);
        sampler.sample(SigarMeasure.MEM_SHARE,       procMem.getShare(),       timestamp);
        
        return null;
    }

    public static final SingleProbeFactory<Long> FACTORY = new SingleProbeFactory<Long>() {
        @Override
        public Runnable newProbe(Long pid, SampleSchema schema) {
            schema.setStatic(ProbeMeasure.PID, pid);
            return new SafeCallable<Void>(new ProcMemProbe(pid, schema));
        }
    };
    
    @Override
    public String toString() {
        return F("%s[pid=%d]", ProcMemProbe.class.getSimpleName(), pid);
    }
}

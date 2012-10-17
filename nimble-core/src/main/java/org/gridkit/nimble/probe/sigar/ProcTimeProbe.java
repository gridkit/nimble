package org.gridkit.nimble.probe.sigar;

import static org.gridkit.nimble.util.StringOps.F;

import java.util.concurrent.Callable;

import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.probe.ProbeMeasure;
import org.gridkit.nimble.probe.RateSampler;
import org.gridkit.nimble.probe.ProbeOps.SingleProbeFactory;
import org.gridkit.nimble.util.SafeCallable;
import org.hyperic.sigar.ProcTime;

public class ProcTimeProbe extends SigarHolder implements Callable<Void> {        
    public static final String PROBE_TYPE = "procTime";
    
    private final long pid;
    private final RateSampler sampler;
    
    public ProcTimeProbe(long pid, SampleSchema schema) {
        this.pid = pid;
        this.sampler = new RateSampler(schema, SigarMeasure.MEASURE_NAME, SigarMeasure.PROBE_TYPE, PROBE_TYPE);
    }
    
    @Override
    public Void call() throws Exception {
        long timestamp = System.currentTimeMillis();

        ProcTime procTime = getSigar().getProcTime(pid);

        sampler.sample(SigarMeasure.CPU_USER,   procTime.getUser(),  timestamp);
        sampler.sample(SigarMeasure.CPU_SYSTEM, procTime.getSys(),   timestamp);
        sampler.sample(SigarMeasure.CPU_TOTAL,  procTime.getTotal(), timestamp);
        
        return null;
    }

    public static final SingleProbeFactory<Long> FACTORY = new SingleProbeFactory<Long>() {
        @Override
        public Runnable newProbe(Long pid, SampleSchema schema) {
            schema.setStatic(ProbeMeasure.PID, pid);
            return new SafeCallable<Void>(new ProcTimeProbe(pid, schema));
        }
    };
    
    @Override
    public String toString() {
        return F("%s[pid=%d]", ProcTimeProbe.class.getSimpleName(), pid);
    }
}

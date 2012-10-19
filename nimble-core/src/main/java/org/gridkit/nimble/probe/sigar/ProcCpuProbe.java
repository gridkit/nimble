package org.gridkit.nimble.probe.sigar;

import static org.gridkit.nimble.util.StringOps.F;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.gridkit.nimble.metering.PointSampler;
import org.gridkit.nimble.probe.RateSampler;
import org.gridkit.nimble.probe.SamplerFactory;
import org.hyperic.sigar.ProcTime;

public class ProcCpuProbe extends SigarHolder implements Callable<Void> {            
    private final long pid;

    private final PointSampler userSampler;
    private final PointSampler systemSampler;
    private final PointSampler totalSampler;
    
    public ProcCpuProbe(long pid, SamplerFactory factory) {
        this.pid = pid;

        this.userSampler = new RateSampler(factory.getSpanSampler(SigarMeasure.CPU_USER));
        this.systemSampler = new RateSampler(factory.getSpanSampler(SigarMeasure.CPU_SYSTEM));
        this.totalSampler = new RateSampler(factory.getSpanSampler(SigarMeasure.CPU_TOTAL));
    }
    
    @Override
    public Void call() throws Exception {
        long timestamp = System.nanoTime();

        ProcTime procTime = getSigar().getProcTime(pid);

        userSampler.write(TimeUnit.MILLISECONDS.toNanos(procTime.getUser()), timestamp);
        systemSampler.write(TimeUnit.MILLISECONDS.toNanos(procTime.getSys()), timestamp);
        totalSampler.write(TimeUnit.MILLISECONDS.toNanos(procTime.getTotal()), timestamp);
        
        return null;
    }
    
    @Override
    public String toString() {
        return F("%s[pid=%d]", ProcCpuProbe.class.getSimpleName(), pid);
    }
}

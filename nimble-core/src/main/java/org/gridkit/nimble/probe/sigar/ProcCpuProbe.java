package org.gridkit.nimble.probe.sigar;

import static org.gridkit.nimble.util.StringOps.F;

import java.util.concurrent.Callable;

import org.gridkit.nimble.metering.PointSampler;
import org.gridkit.nimble.probe.RateSampler;
import org.gridkit.nimble.probe.SamplerFactory;
import org.gridkit.nimble.util.Seconds;
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
        double timestampS = Seconds.currentTime();

        ProcTime procTime = getSigar().getProcTime(pid);

        userSampler.write(Seconds.fromMillis(procTime.getUser()), timestampS);
        systemSampler.write(Seconds.fromMillis(procTime.getSys()), timestampS);
        totalSampler.write(Seconds.fromMillis(procTime.getTotal()), timestampS);
        
        return null;
    }
    
    @Override
    public String toString() {
        return F("%s[pid=%d]", ProcCpuProbe.class.getSimpleName(), pid);
    }
}

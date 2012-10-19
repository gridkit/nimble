package org.gridkit.nimble.probe.sigar;

import static org.gridkit.nimble.util.StringOps.F;

import java.util.concurrent.Callable;

import org.gridkit.nimble.metering.PointSampler;
import org.gridkit.nimble.probe.SamplerFactory;
import org.hyperic.sigar.ProcMem;

public class ProcMemProbe extends SigarHolder implements Callable<Void> {    
    private final long pid;
    
    private final PointSampler sizeSampler;
    private final PointSampler residentSampler;
    private final PointSampler shareSampler;
    
    public ProcMemProbe(long pid, SamplerFactory factory) {
        this.pid = pid;
        
        this.sizeSampler = factory.getPointSampler(SigarMeasure.MEM_SIZE);
        this.residentSampler = factory.getPointSampler(SigarMeasure.MEM_RESIDENT);
        this.shareSampler = factory.getPointSampler(SigarMeasure.MEM_SHARE);
    }
        
    @Override
    public Void call() throws Exception {
        long timestamp = System.nanoTime();
        
        ProcMem procMem = getSigar().getProcMem(pid);
                
        sizeSampler.write(procMem.getSize(), timestamp);
        residentSampler.write(procMem.getResident(), timestamp);
        shareSampler.write(procMem.getShare(), timestamp);
        
        return null;
    }

    @Override
    public String toString() {
        return F("%s[pid=%d]", ProcMemProbe.class.getSimpleName(), pid);
    }
}

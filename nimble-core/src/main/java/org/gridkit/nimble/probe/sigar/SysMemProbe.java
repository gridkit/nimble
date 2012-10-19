package org.gridkit.nimble.probe.sigar;

import static org.gridkit.nimble.util.StringOps.F;

import java.util.concurrent.Callable;

import org.gridkit.nimble.metering.PointSampler;
import org.gridkit.nimble.probe.SamplerFactory;
import org.hyperic.sigar.Mem;

public class SysMemProbe extends SigarHolder implements Callable<Void> {    
    private final PointSampler ramSampler;
    private final PointSampler usedSampler;
    private final PointSampler freeSampler;
    private final PointSampler actualUsedSampler;
    private final PointSampler actualFreeSampler;
    private final PointSampler totalSampler;
    
    public SysMemProbe(SamplerFactory factory) {
        this.ramSampler = factory.getPointSampler(SigarMeasure.MEM_RAM);
        this.usedSampler = factory.getPointSampler(SigarMeasure.MEM_USED);
        this.freeSampler = factory.getPointSampler(SigarMeasure.MEM_FREE);
        this.actualUsedSampler = factory.getPointSampler(SigarMeasure.MEM_ACTUAL_USED);
        this.actualFreeSampler = factory.getPointSampler(SigarMeasure.MEM_ACTUAL_FREE);
        this.totalSampler = factory.getPointSampler(SigarMeasure.MEM_TOTAL);
    }
    
    @Override
    public Void call() throws Exception {
        long timestamp = System.nanoTime();
        
        Mem mem = getSigar().getMem();
        
        ramSampler.write(mem.getRam(), timestamp);
        usedSampler.write(mem.getUsed(), timestamp);
        freeSampler.write(mem.getFree(), timestamp);
        actualUsedSampler.write(mem.getActualUsed(), timestamp);
        actualFreeSampler.write(mem.getActualFree(), timestamp);
        totalSampler.write(mem.getTotal(), timestamp);
        
        return null;
    }

    @Override
    public String toString() {
        return F(SysMemProbe.class.getSimpleName());
    }
}

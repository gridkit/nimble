package org.gridkit.nimble.probe.sigar;

import static org.gridkit.nimble.util.StringOps.F;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.gridkit.nimble.metering.PointSampler;
import org.gridkit.nimble.probe.RateSampler;
import org.gridkit.nimble.probe.SamplerFactory;
import org.hyperic.sigar.Cpu;

public class SysCpuProbe extends SigarHolder implements Callable<Void> {    
    private final PointSampler userSampler;
    private final PointSampler systemSampler;
    private final PointSampler niceSampler;
    private final PointSampler idleSampler;
    private final PointSampler waitSampler;
    private final PointSampler irqSampler;
    private final PointSampler softirqSampler;
    private final PointSampler stolenSampler;
    private final PointSampler totalSampler;
    
    public SysCpuProbe(SamplerFactory factory) {
        this.userSampler = new RateSampler(factory.getSpanSampler(SigarMeasure.CPU_USER));
        this.systemSampler = new RateSampler(factory.getSpanSampler(SigarMeasure.CPU_SYSTEM));
        this.niceSampler = new RateSampler(factory.getSpanSampler(SigarMeasure.CPU_NICE));
        this.idleSampler = new RateSampler(factory.getSpanSampler(SigarMeasure.CPU_IDLE));
        this.waitSampler = new RateSampler(factory.getSpanSampler(SigarMeasure.CPU_WAIT));
        this.irqSampler = new RateSampler(factory.getSpanSampler(SigarMeasure.CPU_IRQ));
        this.softirqSampler = new RateSampler(factory.getSpanSampler(SigarMeasure.CPU_SOFTIRQ));
        this.stolenSampler = new RateSampler(factory.getSpanSampler(SigarMeasure.CPU_STOLEN));
        this.totalSampler = new RateSampler(factory.getSpanSampler(SigarMeasure.CPU_TOTAL));
    }
    
    @Override
    public Void call() throws Exception {
        long timestamp = System.nanoTime();
        
        Cpu cpu = getSigar().getCpu();
        
        userSampler.write(TimeUnit.MILLISECONDS.toNanos(cpu.getUser()), timestamp);
        systemSampler.write(TimeUnit.MILLISECONDS.toNanos(cpu.getSys()), timestamp);
        niceSampler.write(TimeUnit.MILLISECONDS.toNanos(cpu.getNice()), timestamp);
        idleSampler.write(TimeUnit.MILLISECONDS.toNanos(cpu.getIdle()), timestamp);
        waitSampler.write(TimeUnit.MILLISECONDS.toNanos(cpu.getWait()), timestamp);
        irqSampler.write(TimeUnit.MILLISECONDS.toNanos(cpu.getIrq()), timestamp);
        softirqSampler.write(TimeUnit.MILLISECONDS.toNanos(cpu.getSoftIrq()), timestamp);
        stolenSampler.write(TimeUnit.MILLISECONDS.toNanos(cpu.getStolen()), timestamp);
        totalSampler.write(TimeUnit.MILLISECONDS.toNanos(cpu.getTotal()), timestamp);

        return null;
    }

    @Override
    public String toString() {
        return F(SysCpuProbe.class.getSimpleName());
    }
}

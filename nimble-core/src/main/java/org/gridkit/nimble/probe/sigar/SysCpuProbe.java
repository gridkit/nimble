package org.gridkit.nimble.probe.sigar;

import static org.gridkit.nimble.util.StringOps.F;

import java.util.concurrent.Callable;

import org.gridkit.nimble.metering.PointSampler;
import org.gridkit.nimble.probe.RateSampler;
import org.gridkit.nimble.probe.SamplerFactory;
import org.gridkit.nimble.util.Seconds;
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
        double timestampS = Seconds.currentTime();
        
        Cpu cpu = getSigar().getCpu();
        
        userSampler.write(Seconds.fromMillis(cpu.getUser()), timestampS);
        systemSampler.write(Seconds.fromMillis(cpu.getSys()), timestampS);
        niceSampler.write(Seconds.fromMillis(cpu.getNice()), timestampS);
        idleSampler.write(Seconds.fromMillis(cpu.getIdle()), timestampS);
        waitSampler.write(Seconds.fromMillis(cpu.getWait()), timestampS);
        irqSampler.write(Seconds.fromMillis(cpu.getIrq()), timestampS);
        softirqSampler.write(Seconds.fromMillis(cpu.getSoftIrq()), timestampS);
        stolenSampler.write(Seconds.fromMillis(cpu.getStolen()), timestampS);
        totalSampler.write(Seconds.fromMillis(cpu.getTotal()), timestampS);

        return null;
    }

    @Override
    public String toString() {
        return F(SysCpuProbe.class.getSimpleName());
    }
}

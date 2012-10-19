package org.gridkit.nimble.probe.sigar;

import static org.gridkit.nimble.util.StringOps.F;

import java.util.concurrent.Callable;

import org.gridkit.nimble.metering.PointSampler;
import org.gridkit.nimble.probe.RateSampler;
import org.gridkit.nimble.probe.SamplerFactory;
import org.hyperic.sigar.NetInterfaceStat;

public class NetInterfaceProbe extends SigarHolder implements Callable<Void> {     
    private final String interfaceName;

    private final PointSampler rxBytes;
    private final PointSampler rxPackets;
    private final PointSampler rxErrors;
    private final PointSampler rxDropped;
    
    private final PointSampler txBytes;
    private final PointSampler txPackets;
    private final PointSampler txErrors;
    private final PointSampler txDropped;
    
    public NetInterfaceProbe(String interfaceName, SamplerFactory factory) {
        this.interfaceName = interfaceName;
        
        this.rxBytes = new RateSampler(factory.getSpanSampler(SigarMeasure.NET_RX_BYTES));
        this.rxPackets = new RateSampler(factory.getSpanSampler(SigarMeasure.NET_RX_PACKETS));
        this.rxErrors = new RateSampler(factory.getSpanSampler(SigarMeasure.NET_RX_ERRORS));
        this.rxDropped = new RateSampler(factory.getSpanSampler(SigarMeasure.NET_RX_DROPPED));

        this.txBytes = new RateSampler(factory.getSpanSampler(SigarMeasure.NET_TX_BYTES));
        this.txPackets = new RateSampler(factory.getSpanSampler(SigarMeasure.NET_TX_PACKETS));
        this.txErrors = new RateSampler(factory.getSpanSampler(SigarMeasure.NET_TX_ERRORS));
        this.txDropped = new RateSampler(factory.getSpanSampler(SigarMeasure.NET_TX_DROPPED));
    }

    @Override
    public Void call() throws Exception {
        long timestamp = System.nanoTime();
        
        NetInterfaceStat stats = getSigar().getNetInterfaceStat(interfaceName);
        
        rxBytes.write(stats.getRxBytes(), timestamp);
        rxPackets.write(stats.getRxPackets(), timestamp);
        rxErrors.write(stats.getRxErrors(), timestamp);
        rxDropped.write(stats.getRxDropped(), timestamp);
        
        txBytes.write(stats.getTxBytes(), timestamp);
        txPackets.write(stats.getTxPackets(), timestamp);
        txErrors.write(stats.getTxErrors(), timestamp);
        txDropped.write(stats.getTxDropped(), timestamp);
        
        return null;
    }
        
    @Override
    public String toString() {
        return F("%s[%s]", NetInterfaceProbe.class.getSimpleName(), interfaceName);
    }
}

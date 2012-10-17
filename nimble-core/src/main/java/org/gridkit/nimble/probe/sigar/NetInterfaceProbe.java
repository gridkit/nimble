package org.gridkit.nimble.probe.sigar;

import static org.gridkit.nimble.util.StringOps.F;

import java.util.concurrent.Callable;

import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.probe.ProbeMeasure;
import org.gridkit.nimble.probe.RateSampler;
import org.gridkit.nimble.probe.ProbeOps.SingleProbeFactory;
import org.gridkit.nimble.util.SafeCallable;
import org.hyperic.sigar.NetInterfaceStat;

public class NetInterfaceProbe extends SigarHolder implements Callable<Void> { 
    public static final String PROBE_TYPE = "sysNet";
    
    private String interfaceName;
    private final RateSampler sampler;

    public NetInterfaceProbe(String interfaceName, SampleSchema schema) {
        this.interfaceName = interfaceName;
        this.sampler = new RateSampler(schema, SigarMeasure.MEASURE_NAME, SigarMeasure.PROBE_TYPE, PROBE_TYPE);
    }

    @Override
    public Void call() throws Exception {
        long timestamp = System.currentTimeMillis();
        
        NetInterfaceStat stats = getSigar().getNetInterfaceStat(interfaceName);
        
        sampler.sample(SigarMeasure.NET_RX_BYTES,      stats.getRxBytes(),    timestamp);
        sampler.sample(SigarMeasure.NET_RX_PACKETS,    stats.getRxPackets(),  timestamp);
        sampler.sample(SigarMeasure.NET_RX_ERRORS,     stats.getRxErrors(),   timestamp);
        sampler.sample(SigarMeasure.NET_RX_DROPPED,    stats.getRxDropped(),  timestamp);
        
        sampler.sample(SigarMeasure.NET_TX_BYTES,      stats.getTxBytes(),      timestamp);
        sampler.sample(SigarMeasure.NET_TX_PACKETS,    stats.getTxPackets(),    timestamp);
        sampler.sample(SigarMeasure.NET_TX_ERRORS,     stats.getTxErrors(),     timestamp);
        sampler.sample(SigarMeasure.NET_TX_DROPPED,    stats.getTxDropped(),    timestamp);
        
        return null;
    }
    
    public static final SingleProbeFactory<String> FACTORY = new SingleProbeFactory<String>() {
        @Override
        public Runnable newProbe(String interfaceName, SampleSchema schema) {
            schema.setStatic(ProbeMeasure.NET_INTERFACE, interfaceName);
            return new SafeCallable<Void>(new NetInterfaceProbe(interfaceName, schema));
        }
    };
    
    @Override
    public String toString() {
        return F("%s[%s]", NetInterfaceProbe.class.getSimpleName(), interfaceName);
    }
}

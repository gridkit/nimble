package org.gridkit.nimble.probe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.gridkit.nimble.driver.MeteringDriver;
import org.gridkit.nimble.metering.SampleSchema;

public class ProbeOps {
    public interface SingleProbeFactory<P> {
        Runnable newProbe(P param, SampleSchema schema);
    }

    public static <P> List<Runnable> instantiate(Collection<P> params, SingleProbeFactory<P> factory, MeteringDriver metering) {
        List<Runnable> probes = new ArrayList<Runnable>();
        
        for (P param : params) {
            probes.add(factory.newProbe(param, metering.getSchema()));
        }

        return probes;
    }
}

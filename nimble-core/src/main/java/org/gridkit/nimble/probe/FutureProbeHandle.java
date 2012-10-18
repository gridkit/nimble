package org.gridkit.nimble.probe;

import java.util.Collection;
import java.util.concurrent.Future;

public class FutureProbeHandle implements ProbeHandle {
    private final Collection<Future<?>> futures;
    
    public FutureProbeHandle(Collection<Future<?>> futures) {
        this.futures = futures;
    }

    @Override
    public void stop() {
        for (Future<?> future : futures) {
            future.cancel(true);
        }
    }
}
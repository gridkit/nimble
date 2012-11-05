package org.gridkit.nimble.probe;

import java.util.Collection;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
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

    @Override
    public void join() {
        try {
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    throw new RuntimeException(e.getCause());
                } catch (InterruptedException e) {
                    // ignored
                } catch (CancellationException e) {
                    // ignored
                }
            }
        } finally {
            stop();
        }
    }
}

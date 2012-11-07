package org.gridkit.nimble.probe;

import java.util.HashMap;
import java.util.Map;

import org.gridkit.nimble.util.Seconds;

/**
 * Not thread safe class for reporting operations statistics
 */
public class OperationReporter {
    private final SamplerFactory factory;

    private final Map<String, Long> startTimesMs = new HashMap<String, Long>();
    private final Map<String, Long> startTimesNs = new HashMap<String, Long>();

    public OperationReporter(SamplerFactory factory) {
        this.factory = new CachingSamplerFactory(factory);
    }
    
    public void start(String operation) {
        startTimesMs.put(operation, System.currentTimeMillis());
        startTimesNs.put(operation, System.nanoTime());
    }

    public void finish(String operation) {
        long finishNs = System.nanoTime();
        
        if (!startTimesMs.containsKey(operation) || !startTimesNs.containsKey(operation)) {
            throw new IllegalStateException();
        }
        
        long startMs = startTimesMs.get(operation);
        long startNs = startTimesNs.get(operation);
        
        factory.getSpanSampler(operation).write(1.0, Seconds.fromMillis(startMs), Seconds.fromNanos(finishNs - startNs));
        
        startTimesMs.remove(operation);
        startTimesNs.remove(operation);
    }
    
    public void scalar(String key, double value) {
        factory.getScalarSampler(key).write(value);
    }
}

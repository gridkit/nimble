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

    public void finish(String operation, double unit) {
        long finishNs = System.nanoTime();
        
        if (!startTimesMs.containsKey(operation) || !startTimesNs.containsKey(operation)) {
            throw new IllegalStateException();
        }
        
        long startMs = startTimesMs.get(operation);
        long startNs = startTimesNs.get(operation);
        
        try {
            factory.getSpanSampler(operation).write(
                unit, Seconds.fromMillis(startMs), Seconds.fromNanos(finishNs - startNs)
            );
        } finally {
            startTimesMs.remove(operation);
            startTimesNs.remove(operation);
        }
    }
    
    public void finish(String operation) {
        finish(operation, 1.0);
    }
    
    public void scalar(String key, double value) {
        factory.getScalarSampler(key).write(value);
    }
    
    public void event(String key, double measure) {
        factory.getPointSampler(key).write(measure, Seconds.currentTime());
    }
    
    public void event(String key) {
        event(key, 1.0);
    }
}

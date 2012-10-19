package org.gridkit.nimble.probe;

import java.util.HashMap;
import java.util.Map;

import org.gridkit.nimble.metering.PointSampler;
import org.gridkit.nimble.metering.ScalarSampler;
import org.gridkit.nimble.metering.SpanSampler;

public class CachingSamplerFactory implements SamplerFactory {
    private final SamplerFactory delegate;
    
    private final Map<String, ScalarSampler> scalarSamplers = new HashMap<String, ScalarSampler>();
    private final Map<String, PointSampler> pointSamplers = new HashMap<String, PointSampler>();
    private final Map<String, SpanSampler> spanSamplers = new HashMap<String, SpanSampler>();
    
    public CachingSamplerFactory(SamplerFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public ScalarSampler getScalarSampler(String key) {
        if (!scalarSamplers.containsKey(key)) {
            scalarSamplers.put(key, delegate.getScalarSampler(key));
        }
        
        return scalarSamplers.get(key);
    }

    @Override
    public PointSampler getPointSampler(String key) {
        if (!pointSamplers.containsKey(key)) {
            pointSamplers.put(key, delegate.getPointSampler(key));
        }
        
        return pointSamplers.get(key);
    }

    @Override
    public SpanSampler getSpanSampler(String key) {
        if (!spanSamplers.containsKey(key)) {
            spanSamplers.put(key, delegate.getSpanSampler(key));
        }
        
        return spanSamplers.get(key);
    }
}

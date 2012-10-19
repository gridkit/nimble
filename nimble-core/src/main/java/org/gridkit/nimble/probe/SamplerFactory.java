package org.gridkit.nimble.probe;

import org.gridkit.nimble.metering.PointSampler;
import org.gridkit.nimble.metering.ScalarSampler;
import org.gridkit.nimble.metering.SpanSampler;

public interface SamplerFactory {
    ScalarSampler getScalarSampler(String key);
    
    PointSampler getPointSampler(String key);
    
    SpanSampler getSpanSampler(String key);
}

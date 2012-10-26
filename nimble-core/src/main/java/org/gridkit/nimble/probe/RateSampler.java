package org.gridkit.nimble.probe;

import org.gridkit.nimble.metering.PointSampler;
import org.gridkit.nimble.metering.SpanSampler;

public class RateSampler implements PointSampler {
    private final SpanSampler delegate;

    private double prevValue;
    private double prevTimestampS;
    
    private boolean firstStored = false;

    public RateSampler(SpanSampler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void write(double value, double timestampS) {        
        if (firstStored) {
            delegate.write(value - prevValue, prevTimestampS, timestampS - prevTimestampS);
        } else {
            firstStored = true;
        }
        
        prevValue = value;
        prevTimestampS = timestampS;
    }
}

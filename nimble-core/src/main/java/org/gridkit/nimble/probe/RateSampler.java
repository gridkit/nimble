package org.gridkit.nimble.probe;

import org.gridkit.nimble.metering.PointSampler;
import org.gridkit.nimble.metering.SpanSampler;

public class RateSampler implements PointSampler {
    private final SpanSampler delegate;

    private double prevValue;
    private long prevTimestamp;
    
    private boolean firstStored = false;

    public RateSampler(SpanSampler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void write(double value, long nanotimestamp) {        
        if (firstStored) {
            delegate.write(value - prevValue, prevTimestamp, nanotimestamp);
        } else {
            firstStored = true;
        }
        
        prevValue = value;
        prevTimestamp = nanotimestamp;
    }
}

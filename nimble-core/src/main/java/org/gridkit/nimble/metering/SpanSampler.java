package org.gridkit.nimble.metering;

public interface SpanSampler {
    public void write(double value, double timestampS, double durationS);
}

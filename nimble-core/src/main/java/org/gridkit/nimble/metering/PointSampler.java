package org.gridkit.nimble.metering;

public interface PointSampler {
    public void write(double value, double timestampS);
}

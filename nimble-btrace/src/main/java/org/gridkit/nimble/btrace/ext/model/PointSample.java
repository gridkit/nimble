package org.gridkit.nimble.btrace.ext.model;

public class PointSample extends ScalarSample {
    private static final long serialVersionUID = 8730181924576871767L;

    private long timestampMs;
    
    public long getTimestampMs() {
        return timestampMs;
    }

    public void setTimestampMs(long timestamp) {
        this.timestampMs = timestamp;
    }
}

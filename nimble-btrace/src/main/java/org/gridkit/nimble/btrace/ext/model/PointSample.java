package org.gridkit.nimble.btrace.ext.model;

public class PointSample extends ScalarSample {
    private static final long serialVersionUID = 8730181924576871767L;

    private long timestamp;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
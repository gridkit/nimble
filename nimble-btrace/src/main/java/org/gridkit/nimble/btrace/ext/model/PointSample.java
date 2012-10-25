package org.gridkit.nimble.btrace.ext.model;

public class PointSample extends ScalarSample {
    private static final long serialVersionUID = 8730181924576871767L;

    private long timestampMs;

    /**
     * if true, value is interpreted as measure of some unit since beginning of the measurement (like CPU time, bytes sent)
     */
    private boolean rate;
    
    /**
     * if true, value is interpreted as duration in nanoseconds
     */
    private boolean duration;
    
    public long getTimestampMs() {
        return timestampMs;
    }

    public void setTimestampMs(long timestamp) {
        this.timestampMs = timestamp;
    }

    public boolean isRate() {
        return rate;
    }

    public void setRate(boolean rate) {
        this.rate = rate;
    }

    public boolean isDuration() {
        return duration;
    }

    public void setDuration(boolean duration) {
        this.duration = duration;
    }
}

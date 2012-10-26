package org.gridkit.nimble.btrace.ext.model;

public class SpanSample extends PointSample {
    private static final long serialVersionUID = 1321554416280077368L;
    
    private long durationNs;
    
    public long getDurationNs() {
        return durationNs;
    }

    public void setDurationNs(long durationNs) {
        this.durationNs = durationNs;
    }
}

package org.gridkit.nimble.btrace.ext.model;

public class SpanSample extends ScalarSample {
    private static final long serialVersionUID = 1321554416280077368L;
    
    private long startTimestampMs;
    private long finishTimestampMs;
    
    public long getStartTimestampMs() {
        return startTimestampMs;
    }
    
    public void setStartTimestampMs(long startTimestampMs) {
        this.startTimestampMs = startTimestampMs;
    }
    
    public long getFinishTimestampMs() {
        return finishTimestampMs;
    }
    
    public void setFinishTimestampMs(long finishTimestampMs) {
        this.finishTimestampMs = finishTimestampMs;
    }
}

package org.gridkit.nimble.btrace.ext.model;

public class SpanSample extends Sample {
    private static final long serialVersionUID = 1321554416280077368L;
    
    private long startTimestamp;
    private long finishTimestamp;
    
    public long getStartTimestamp() {
        return startTimestamp;
    }
    
    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }
    
    public long getFinishTimestamp() {
        return finishTimestamp;
    }
    
    public void setFinishTimestamp(long finishTimestamp) {
        this.finishTimestamp = finishTimestamp;
    }
}

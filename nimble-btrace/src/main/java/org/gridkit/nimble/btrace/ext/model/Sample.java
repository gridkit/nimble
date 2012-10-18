package org.gridkit.nimble.btrace.ext.model;

import java.io.Serializable;

public class Sample implements Serializable {
    private static final long serialVersionUID = -6018384733412548214L;
    
    private long seqNumber;
    private Number value;
    
    public Number getValue() {
        return value;
    }

    public void setValue(Number value) {
        this.value = value;
    }

    public long getSeqNumber() {
        return seqNumber;
    }

    public void setSeqNumber(long seqNumber) {
        this.seqNumber = seqNumber;
    }
}

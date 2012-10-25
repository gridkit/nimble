package org.gridkit.nimble.btrace.ext.model;

import java.io.Serializable;

public class SampleStoreState implements Serializable {
    private static final long serialVersionUID = -539078122060048858L;
    
    private String scriptClass;
    private String sampleStore;
    private long nextSeqNum;
    
    public String getScriptClass() {
        return scriptClass;
    }
    
    public void setScriptClass(String scriptClass) {
        this.scriptClass = scriptClass;
    }
    
    public String getSampleStore() {
        return sampleStore;
    }
    
    public void setSampleStore(String sampleStore) {
        this.sampleStore = sampleStore;
    }
    
    public long getNextSeqNum() {
        return nextSeqNum;
    }
    
    public void setNextSeqNum(long nextSeqNum) {
        this.nextSeqNum = nextSeqNum;
    }
}

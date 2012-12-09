package org.gridkit.nimble.btrace.ext.model;

import java.io.Serializable;
import java.util.List;

public class SampleStoreContents implements Serializable {
    private static final long serialVersionUID = -7906160410947353935L;
    
    private String sampleStore;
    private List<ScalarSample> samples;
        
    public String getSampleStore() {
        return sampleStore;
    }
    
    public void setSampleStore(String sampleStore) {
        this.sampleStore = sampleStore;
    }
    
    public List<ScalarSample> getSamples() {
        return samples;
    }
    
    public void setSamples(List<ScalarSample> samples) {
        this.samples = samples;
    }
}

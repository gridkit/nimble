package org.gridkit.nimble.btrace.ext;

import java.io.Serializable;
import java.util.List;

import org.gridkit.nimble.btrace.ext.model.Sample;

public class PollSamplesCmdResult<S extends Sample> implements Serializable {
    private static final long serialVersionUID = 8988203060254970517L;

    private final Class<S> clazz;
    private final List<S> samples;
    
    public PollSamplesCmdResult(SampleStore<S> store) {
        this.clazz = store.getSampleClass();
        this.samples = store.getSamples();
    }
    
    public Class<S> getSampleClass() {
        return clazz;
    }
    
    public List<S> getSamples() {
        return samples;
    }    
}

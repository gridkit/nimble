package org.gridkit.nimble.btrace.ext;

import java.util.ArrayList;
import java.util.List;

import org.gridkit.nimble.btrace.ext.model.Sample;

public class SampleStore<S extends Sample> {
    private final Class<S> clazz;
    private final RingBuffer<S> buffer;
    
    public SampleStore(Class<S> clazz, int capacity) {
        this.clazz = clazz;
        this.buffer = new RingBuffer<S>(capacity);
    }
    
    public void add(S sample) {
        buffer.add(sample);
    }
    
    public List<S> getSamples() {
        List<RingBuffer.Element<S>> elements = buffer.get();
        
        List<S> result = new ArrayList<S>(elements.size());
        
        for (RingBuffer.Element<S> element : elements) {
            S sample = element.getData();
            sample.setSeqNumber(element.getSeqNumber());
            result.add(sample);
        }
        
        return result;
    }
    
    public Class<S> getSampleClass() {
        return clazz;
    }
}

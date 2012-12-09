package org.gridkit.nimble.btrace.ext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.gridkit.nimble.btrace.ext.model.ScalarSample;

public class SampleStore {
    private final String name;
    private volatile RingBuffer<ScalarSample> buffer;
    
    protected SampleStore(String name, int capacity) {
        this.name = name;
        this.buffer = new RingBuffer<ScalarSample>(capacity);
    }
    
    protected void add(ScalarSample sample) {
        RingBuffer<ScalarSample> buffer = this.buffer;
        
        if (buffer != null) {
            buffer.add(sample);
        }
    }
    
    protected List<ScalarSample> getSamples() {
        RingBuffer<ScalarSample> buffer = this.buffer;
        
        if (buffer != null) {
            List<RingBuffer.Element<ScalarSample>> elements = buffer.get();
            
            List<ScalarSample> result = new ArrayList<ScalarSample>(elements.size());
            
            for (RingBuffer.Element<ScalarSample> element : elements) {
                ScalarSample sample = element.getData();
                sample.setSeqNumber(element.getSeqNumber());
                result.add(sample);
            }
            
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    protected String getName() {
        return name;
    }
    
    protected void close() {
        buffer = null;
    }
}

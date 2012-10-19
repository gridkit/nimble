package org.gridkit.nimble.btrace.ext;

import java.util.ArrayList;
import java.util.List;

import org.gridkit.nimble.btrace.ext.model.ScalarSample;

public class SampleStore {
    private final String name;
    private final RingBuffer<ScalarSample> buffer;
    
    public SampleStore(String name, int capacity) {
        this.name = name;
        this.buffer = new RingBuffer<ScalarSample>(capacity);
    }
    
    public void add(ScalarSample sample) {
        buffer.add(sample);
    }
    
    public List<ScalarSample> getSamples() {
        List<RingBuffer.Element<ScalarSample>> elements = buffer.get();
        
        List<ScalarSample> result = new ArrayList<ScalarSample>(elements.size());
        
        for (RingBuffer.Element<ScalarSample> element : elements) {
            ScalarSample sample = element.getData();
            sample.setSeqNumber(element.getSeqNumber());
            result.add(sample);
        }
        
        return result;
    }

    public String getName() {
        return name;
    }
}

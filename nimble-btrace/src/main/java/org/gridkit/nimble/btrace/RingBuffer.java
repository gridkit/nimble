package org.gridkit.nimble.btrace;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class RingBuffer {
    @SuppressWarnings("serial")
    public static class Element implements Serializable {
        private long id;
        private Object data;
        
        public long getId() {
            return id;
        }
        
        public void setId(long id) {
            this.id = id;
        }
        
        public Object getData() {
            return data;
        }
        
        public void setData(Object data) {
            this.data = data;
        }
    }
    
    public static final long START_ID = 0;
    
    private final AtomicReferenceArray<Element> elements;
    
    private AtomicLong writeCounter = new AtomicLong(START_ID);

    private long readCounter = START_ID;
    
    public RingBuffer(int capacity) {
        this.elements = new AtomicReferenceArray<Element>(capacity);
    }
    
    public long add(Object data) {
        Element element = new Element();
        
        long id = writeCounter.getAndIncrement();
        
        element.setId(id);
        element.setData(data);

        elements.set(index(id), element);
        
        return id;
    }
    
    public synchronized List<Element> get() {
        List<Element> result = new ArrayList<RingBuffer.Element>();
        
        while(result.size() < elements.length()) {
            Element element = elements.get(index(readCounter));
            
            if (element == null || element.id < readCounter) {
                break;
            } else if (element.id == readCounter) {
                result.add(element);
                readCounter += 1;
            } else { // (element.id > id)
                readCounter = element.id - elements.length() + 1; // oldest id before element
            }
        }
        
        return result;
    }
    
    private int index(long id) {
        return (int)(id % elements.length());
    }
}

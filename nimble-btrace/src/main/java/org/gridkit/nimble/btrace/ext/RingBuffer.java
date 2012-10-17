package org.gridkit.nimble.btrace.ext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class RingBuffer<T> {
    @SuppressWarnings("serial")
    public static class Element<T> implements Serializable {
        private long id;
        private T data;
        
        public long getId() {
            return id;
        }
        
        public void setId(long id) {
            this.id = id;
        }
        
        public Object getData() {
            return data;
        }
        
        public void setData(T data) {
            this.data = data;
        }
    }
    
    public static final long START_ID = 0;
    
    private final AtomicReferenceArray<Element<T>> elements;
    
    private AtomicLong writeCounter = new AtomicLong(START_ID);

    private long readCounter = START_ID;
    
    public RingBuffer(int capacity) {
        this.elements = new AtomicReferenceArray<Element<T>>(capacity);
    }
    
    public long add(T data) {
        Element<T> element = new Element<T>();
        
        long id = writeCounter.getAndIncrement();
        
        element.setId(id);
        element.setData(data);

        elements.set(index(id), element);
        
        return id;
    }
    
    public synchronized List<Element<T>> get() {
        List<Element<T>> result = new ArrayList<RingBuffer.Element<T>>();
        
        while(result.size() < elements.length()) {
            Element<T> element = elements.get(index(readCounter));
            
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

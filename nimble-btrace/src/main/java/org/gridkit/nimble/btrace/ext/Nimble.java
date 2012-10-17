package org.gridkit.nimble.btrace.ext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.java.btrace.api.extensions.BTraceExtension;

@BTraceExtension
public class Nimble {
    private static ConcurrentMap<String, RingBuffer> ringBuffers = new ConcurrentHashMap<String, RingBuffer>();

    protected static Map<String, RingBuffer> getRingBuffers() {
        Map<String, RingBuffer> result = new HashMap<String, RingBuffer>();
        result.putAll(ringBuffers);
        return result;
    }
    
    public static RingBuffer newRingBuffer(String id, int capacity) {
        RingBuffer result = new RingBuffer(capacity);
        ringBuffers.put(id, result);
        return result;
    }
    
    public static <E> void put(RingBuffer ringBuffer, Object data) {
        ringBuffer.add(data);
    }
    
    public static Map<String, Object> newSample() {
        return new HashMap<String, Object>();
    }
    
    public static void put(Map<String, Object> sample, String key, Object value) {
        sample.put(key, value);
    }
}

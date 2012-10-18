package org.gridkit.nimble.btrace.ext;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.java.btrace.api.extensions.BTraceExtension;
import net.java.btrace.ext.Printer;

import org.gridkit.nimble.btrace.ext.model.Sample;
import org.gridkit.nimble.btrace.ext.model.SpanSample;
import org.gridkit.nimble.btrace.ext.model.TimestampSample;

@BTraceExtension
public class Nimble {
    private static ConcurrentMap<String, SampleStore<?>> sampleStores = new ConcurrentHashMap<String, SampleStore<?>>();

    protected static ConcurrentMap<String, SampleStore<?>> getSampleStores() {
        return sampleStores;
    }
    
    public static SampleStore<Sample> newSampleStore(String name, int capacity) {
        return newSampleStore(name, capacity, Sample.class);
    }
    
    public static SampleStore<TimestampSample> newTimestampSampleStore(String name, int capacity) {
        return newSampleStore(name, capacity, TimestampSample.class);
    }
    
    public static SampleStore<SpanSample> newSpanSampleStore(String name, int capacity) {
        return newSampleStore(name, capacity, SpanSample.class);
    }
    
    private static <S extends Sample> SampleStore<S> newSampleStore(String name, int capacity, Class<S> clazz) {
        SampleStore<S> result = new SampleStore<S>(clazz, capacity);

        if (sampleStores.put(name, result) != null) {
            Printer.print("Replacing existing sample store for name '" + name + "'");
        }

        return result;
    }

    public static void sample(SampleStore<Sample> store, Number value) {
        Sample sample = new Sample();
        
        sample.setValue(value);
        store.add(sample);
        
    }
    
    public static void sample(SampleStore<TimestampSample> store, Number value, long timestamp) {
        TimestampSample sample = new TimestampSample();
        
        sample.setValue(value);
        sample.setTimestamp(timestamp);
        
        store.add(sample);
    }

    public static void sample(SampleStore<SpanSample> store, Number value, long startTimestamp, long finishTimestamp) {
        SpanSample sample = new SpanSample();
        
        sample.setValue(value);
        sample.setStartTimestamp(startTimestamp);
        sample.setFinishTimestamp(finishTimestamp);
        
        store.add(sample);
    }
}

package org.gridkit.nimble.btrace.ext;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.java.btrace.api.extensions.BTraceExtension;

import org.gridkit.nimble.btrace.ext.model.DurationSample;
import org.gridkit.nimble.btrace.ext.model.PointSample;
import org.gridkit.nimble.btrace.ext.model.RateSample;
import org.gridkit.nimble.btrace.ext.model.ScalarSample;
import org.gridkit.nimble.btrace.ext.model.SpanSample;

@BTraceExtension
public class Nimble {
    private static ConcurrentMap<String, ScriptStore> scriptStores = new ConcurrentHashMap<String, ScriptStore>();
    
    protected static Collection<ScriptStore> getScriptStores(Collection<String> scriptClasses) {        
        Map<String, ScriptStore> result = new HashMap<String, ScriptStore>();
        
        result.putAll(scriptStores);
        result.keySet().retainAll(scriptClasses);
        
        return result.values();
    }
        
    public static SampleStore newSampleStore(String name, int capacity) {
        String scriptClass = getScriptClass();
        
        scriptStores.putIfAbsent(scriptClass, new ScriptStore(scriptClass));
        
        return scriptStores.get(scriptClass).add(name, capacity);
    }
    
    // TODO find more correct way to do it
    private static String getScriptClass() {
        StackTraceElement[] stackTrace = (new Exception()).getStackTrace();
        
        String clazz = stackTrace[2].getClassName();
        
        int index = clazz.lastIndexOf('$');
        
        return clazz.substring(0, index);
    }

    public static void scalar(String key, SampleStore store, Number value) {
        ScalarSample sample = new ScalarSample();
        
        sample.setKey(key);
        sample.setValue(value);
        
        store.add(sample);
    }
    
    public static void duration(String key, SampleStore store, long durationNs, long timestampMs) {
        DurationSample sample = new DurationSample();
        
        sample.setKey(key);
        sample.setValue(durationNs);
        sample.setTimestampMs(timestampMs);
        
        store.add(sample);
    }
    
    public static void rate(String key, SampleStore store, Number value, long timestampMs) {
        RateSample sample = new RateSample();
        
        sample.setKey(key);
        sample.setValue(value);
        sample.setTimestampMs(timestampMs);
        
        store.add(sample);
    }
    
    public static void point(String key, SampleStore store, Number value, long timestampMs) {
        PointSample sample = new PointSample();
        
        sample.setKey(key);
        sample.setValue(value);
        sample.setTimestampMs(timestampMs);
        
        store.add(sample);
    }

    public static void span(String key, SampleStore store, Number value, long timestampMs, long durationNs) {
        SpanSample sample = new SpanSample();
        
        sample.setKey(key);
        sample.setValue(value);
        sample.setTimestampMs(timestampMs);
        sample.setDurationNs(durationNs);
        
        store.add(sample);
    }
}

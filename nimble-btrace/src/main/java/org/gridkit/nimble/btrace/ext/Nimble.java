package org.gridkit.nimble.btrace.ext;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.java.btrace.annotations.BTrace;
import net.java.btrace.api.core.BTraceLogger;
import net.java.btrace.api.extensions.BTraceExtension;

import org.gridkit.nimble.btrace.ext.model.DurationSample;
import org.gridkit.nimble.btrace.ext.model.PointSample;
import org.gridkit.nimble.btrace.ext.model.RateSample;
import org.gridkit.nimble.btrace.ext.model.ScalarSample;
import org.gridkit.nimble.btrace.ext.model.SpanSample;

@BTraceExtension
public class Nimble {
    private static ConcurrentMap<String, ScriptStore> scriptStores = new ConcurrentHashMap<String, ScriptStore>();
    
    protected static ScriptStore getScriptStore(String scriptClass) {        
        return scriptStores.get(scriptClass);
    }
    
    protected static void removeScriptStore(String scriptClass) {
        ScriptStore scriptStore = scriptStores.remove(scriptClass);
        
        if (scriptStore != null) {
            scriptStore.close();
        } else {
            BTraceLogger.debugPrint("WARN: Nimble.removeScriptStore: scriptStore == null");
        }
    }
        
    public static SampleStore newSampleStore(String name, int capacity) {
        String scriptClass = getScriptClass();
        
        if (scriptClass != null) {
            scriptStores.putIfAbsent(scriptClass, new ScriptStore(scriptClass));
            return scriptStores.get(scriptClass).addSampleStore(name, capacity);
        } else {
            return null;
        }
    }
    
    @SuppressWarnings("restriction")
    private static String getScriptClass() {
        String result = null;
        int i = 1;
        Class<?> clazz = null;
        
        while ((clazz = sun.reflect.Reflection.getCallerClass(i++)) != null) {
            if (clazz.isAnnotationPresent(BTrace.class)) {
                result = clazz.getName();
                break;
            }
        }
        
        return result;
    }
    
    public static void scalar(String key, SampleStore store, Number value) {
        if (store != null) {
            ScalarSample sample = new ScalarSample();
            
            sample.setKey(key);
            sample.setValue(value);
            
            store.add(sample);
        }
    }
    
    public static void duration(String key, SampleStore store, long durationNs, long timestampMs) {
        if (store != null) {
            DurationSample sample = new DurationSample();
            
            sample.setKey(key);
            sample.setValue(durationNs);
            sample.setTimestampMs(timestampMs);
            
            store.add(sample);
        }
    }
    
    public static void rate(String key, SampleStore store, Number value, long timestampMs) {
        if (store != null) {
            RateSample sample = new RateSample();
            
            sample.setKey(key);
            sample.setValue(value);
            sample.setTimestampMs(timestampMs);
            
            store.add(sample);
        }
    }
    
    public static void point(String key, SampleStore store, Number value, long timestampMs) {
        if (store != null) {
            PointSample sample = new PointSample();
            
            sample.setKey(key);
            sample.setValue(value);
            sample.setTimestampMs(timestampMs);
            
            store.add(sample);
        }
    }

    public static void span(String key, SampleStore store, Number value, long timestampMs, long durationNs) {
        if (store != null) {
            SpanSample sample = new SpanSample();
            
            sample.setKey(key);
            sample.setValue(value);
            sample.setTimestampMs(timestampMs);
            sample.setDurationNs(durationNs);
            
            store.add(sample);
        }
    }
}

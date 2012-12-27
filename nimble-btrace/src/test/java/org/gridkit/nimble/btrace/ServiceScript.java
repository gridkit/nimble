package org.gridkit.nimble.btrace;

import net.java.btrace.annotations.BTrace;
import net.java.btrace.annotations.OnMethod;

import org.gridkit.nimble.btrace.ext.Nimble;
import org.gridkit.nimble.btrace.ext.SampleStore;

@BTrace
public class ServiceScript {
    public static final int STORE_SIZE_BASE = 10;
    public static final String STORE_NAME = "store";
    
    public static SampleStore store = Nimble.newSampleStore(STORE_NAME, STORE_SIZE_BASE * 5);
    
    @OnMethod(clazz="org.gridkit.nimble.btrace.BTraceDriverTest$Service", method="scalar")
    public static void onScalar(Number value) {
        Nimble.scalar("scalar", store, value);
    }
    
    @OnMethod(clazz="org.gridkit.nimble.btrace.BTraceDriverTest$Service", method="duartion")
    public static void onDuartion(long durationNs, long timestampMs) {
        Nimble.duration("duartion", store, durationNs, timestampMs);
    }

    @OnMethod(clazz="org.gridkit.nimble.btrace.BTraceDriverTest$Service", method="rate")
    public static void onRate(Number value, long timestampMs) {
        Nimble.rate("rate", store, value, timestampMs);
    }
    
    @OnMethod(clazz="org.gridkit.nimble.btrace.BTraceDriverTest$Service", method="point")
    public static void onPoint(Number value, long timestampMs) {
        Nimble.point("point", store, value, timestampMs);
    }
    
    @OnMethod(clazz="org.gridkit.nimble.btrace.BTraceDriverTest$Service", method="span")
    public static void onSpan(Number value, long timestampMs, long durationNs) {
        Nimble.span("span", store, value, timestampMs, durationNs);
    }
}

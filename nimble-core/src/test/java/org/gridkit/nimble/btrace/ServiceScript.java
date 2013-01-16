package org.gridkit.nimble.btrace;

import net.java.btrace.annotations.BTrace;
import net.java.btrace.annotations.OnMethod;

import org.gridkit.nimble.btrace.ext.Nimble;
import org.gridkit.nimble.btrace.ext.SampleStore;

@BTrace
public class ServiceScript {
    public static final int STORE_SIZE_BASE = 10;
    
    public static SampleStore scalarStore = Nimble.newSampleStore("SCALAR", STORE_SIZE_BASE);
    public static SampleStore durationStore = Nimble.newSampleStore("DURATION", STORE_SIZE_BASE);
    public static SampleStore rateStore = Nimble.newSampleStore("RATE", STORE_SIZE_BASE);
    public static SampleStore poitStore = Nimble.newSampleStore("POINT", STORE_SIZE_BASE);
    public static SampleStore spanStore = Nimble.newSampleStore("SPAN", STORE_SIZE_BASE);
    
    @OnMethod(clazz="org.gridkit.nimble.btrace.BTraceDriverTest$Service", method="scalar")
    public static void onScalar(Number value) {
    	System.out.println("onScalar");
        Nimble.scalar("scalar", scalarStore, value);
    }
    
    @OnMethod(clazz="org.gridkit.nimble.btrace.BTraceDriverTest$Service", method="duartion")
    public static void onDuartion(long durationNs, long timestampMs) {
    	System.out.println("onDuartion");
        Nimble.duration("duartion", durationStore, durationNs, timestampMs);
    }

    @OnMethod(clazz="org.gridkit.nimble.btrace.BTraceDriverTest$Service", method="rate")
    public static void onRate(Number value, long timestampMs) {
    	System.out.println("onRate");
        Nimble.rate("rate", rateStore, value, timestampMs);
    }
    
    @OnMethod(clazz="org.gridkit.nimble.btrace.BTraceDriverTest$Service", method="point")
    public static void onPoint(Number value, long timestampMs) {
    	System.out.println("onPoint");
        Nimble.point("point", poitStore, value, timestampMs);
    }
    
    @OnMethod(clazz="org.gridkit.nimble.btrace.BTraceDriverTest$Service", method="span")
    public static void onSpan(Number value, long timestampMs, long durationNs) {
    	System.out.println("onSpan");
        Nimble.span("span", spanStore, value, timestampMs, durationNs);
    }
}

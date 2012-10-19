package org.gridkit.nimble.btrace;

import net.java.btrace.annotations.BTrace;
import net.java.btrace.annotations.Duration;
import net.java.btrace.annotations.Kind;
import net.java.btrace.annotations.OnMethod;
import net.java.btrace.annotations.Location;

import org.gridkit.nimble.btrace.ext.Nimble;
import org.gridkit.nimble.btrace.ext.SampleStore;

@BTrace
public class ServiceScript {
    public static SampleStore serveStore = Nimble.newSampleStore("serveStore", 500);
    
    @OnMethod(clazz="org.gridkit.nimble.btrace.BTraceDriverTest$Service", method="serve", location=@Location(value=Kind.RETURN))
    public static void serve(@Duration long dur) {
        Nimble.sample("serveCall", serveStore, dur);
    }
}

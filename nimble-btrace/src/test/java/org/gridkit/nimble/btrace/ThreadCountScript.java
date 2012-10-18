package org.gridkit.nimble.btrace;

import net.java.btrace.annotations.BTrace;
import net.java.btrace.annotations.OnTimer;
import net.java.btrace.ext.Printer;
import net.java.btrace.ext.Threads;
import net.java.btrace.ext.Time;

import org.gridkit.nimble.btrace.ext.Nimble;
import org.gridkit.nimble.btrace.ext.SampleStore;
import org.gridkit.nimble.btrace.ext.model.TimestampSample;

@BTrace
public class ThreadCountScript {
    public static final String THREAD_COUNT_STORE = "threadCountStore";
    
    public static SampleStore<TimestampSample> threadCount = Nimble.newTimestampSampleStore(THREAD_COUNT_STORE, 500);
    
    @OnTimer(50)
    public static void reportThreadCount() {
        Nimble.sample(threadCount, Threads.threadCount(), Time.millis());
    }
    
    @OnTimer(1000)
    public static void printHello() {
        Printer.println(" -- hello from BTrace client --");
    }
}

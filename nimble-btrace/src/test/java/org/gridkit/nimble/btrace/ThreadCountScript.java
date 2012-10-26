package org.gridkit.nimble.btrace;

import net.java.btrace.annotations.BTrace;
import net.java.btrace.annotations.OnTimer;
import net.java.btrace.ext.Printer;
import net.java.btrace.ext.Threads;
import net.java.btrace.ext.Time;

import org.gridkit.nimble.btrace.ext.Nimble;
import org.gridkit.nimble.btrace.ext.SampleStore;

@BTrace
public class ThreadCountScript {
    public static SampleStore threadCount = Nimble.newSampleStore("threadCountStore", 500);
    
    @OnTimer(50)
    public static void reportThreadCount() {
        Nimble.point("threadCount", threadCount, Threads.threadCount(), Time.millis());
    }
    
    @OnTimer(1000)
    public static void printHello() {
        Printer.println(" -- hello from BTrace client --");
    }
}

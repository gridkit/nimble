package org.gridkit.nimble.btrace;

import net.java.btrace.annotations.BTrace;
import net.java.btrace.annotations.OnTimer;
import net.java.btrace.ext.Printer;

@BTrace
public class BTraceClientFactoryTestScript {
    @OnTimer(1000)
    public static void hello() {
        Printer.println(" --- hello --- ");
    }
}

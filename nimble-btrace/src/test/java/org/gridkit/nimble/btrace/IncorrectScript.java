package org.gridkit.nimble.btrace;

import net.java.btrace.annotations.BTrace;
import net.java.btrace.annotations.OnTimer;

@BTrace
public class IncorrectScript {
    @OnTimer(1)
    public static void printHello() {
        System.err.println(IncorrectScript.class.getName());
    }
}

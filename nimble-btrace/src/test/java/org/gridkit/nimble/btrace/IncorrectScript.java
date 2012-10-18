package org.gridkit.nimble.btrace;

import net.java.btrace.annotations.BTrace;
import net.java.btrace.annotations.OnTimer;

@BTrace
public class IncorrectScript {
    @OnTimer(100)
    public static void printHello() {
        System.out.println(IncorrectScript.class.getName());
    }
}

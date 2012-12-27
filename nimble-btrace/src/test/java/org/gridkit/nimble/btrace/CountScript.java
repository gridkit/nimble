package org.gridkit.nimble.btrace;

import net.java.btrace.annotations.BTrace;
import net.java.btrace.annotations.OnMethod;

import org.gridkit.nimble.btrace.ext.Nimble;
import org.gridkit.nimble.btrace.ext.SampleStore;

@BTrace
public class CountScript {
    public static final String STORE_NAME = "ticks";
    public static final int STORE_SIZE = 10;
    public static final String SCALAR_NAME = "tick";
    
    public static SampleStore ticks = Nimble.newSampleStore(STORE_NAME, STORE_SIZE);

    @OnMethod(clazz="org.gridkit.nimble.btrace.Count", method="tick", type = "java.lang.Boolean (int)")
    public static void onTick(int value) {
        Nimble.scalar(SCALAR_NAME, ticks, value);
    }
}

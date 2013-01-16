package org.gridkit.nimble.btrace;

import java.util.concurrent.TimeUnit;

class Seconds {

	private static final double MILLIS = TimeUnit.SECONDS.toMillis(1);
    private static final double NANOS = TimeUnit.SECONDS.toNanos(1);
    
    public static double currentTime() {
        return fromMillis(System.currentTimeMillis());
    }
    
    public static double fromMillis(double millis) {
        return millis / MILLIS;
    }
    
    public static double fromNanos(double nanos) {
        return nanos / NANOS;
    }
    
    public static double toMillis(double seconds) {
        return seconds * MILLIS;
    }
    
    public static double toNanos(double seconds) {
        return seconds * NANOS;
    }
}

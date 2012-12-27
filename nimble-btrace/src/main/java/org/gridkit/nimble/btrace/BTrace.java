package org.gridkit.nimble.btrace;

import java.util.concurrent.TimeUnit;

import org.gridkit.nimble.driver.MeteringAware;

public class BTrace {
    public static BTraceDriver newDriver() {
        return newDriver(4);
    }

    public static BTraceDriver newDriver(int corePoolSize) {
        return newDriver(corePoolSize, TimeUnit.SECONDS.toMillis(3), TimeUnit.SECONDS.toMillis(5));
    }
    
    public static BTraceDriver newDriver(int corePoolSize, long pollDelayMs, long timeoutMs) {
        return newDriver(corePoolSize, pollDelayMs, timeoutMs, new BTraceClientSettings());
    }
    
    public static BTraceDriver newDriver(int corePoolSize, long pollDelayMs, long timeoutMs, BTraceClientSettings settings) {
        return new BTraceDriver.Impl(corePoolSize, pollDelayMs, timeoutMs, settings);
    }
    
    public static MeteringAware<BTraceSamplerFactoryProvider> defaultReporter() {
        return new StandardBTraceSamplerFactoryProvider();
    }
}

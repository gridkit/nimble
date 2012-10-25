package org.gridkit.nimble.btrace;

import java.util.concurrent.TimeUnit;

import org.gridkit.nimble.driver.MeteringAware;

public class BTrace {
    public static BTraceDriver newDriver() {
        return newDriver(16, TimeUnit.SECONDS.toMillis(3), TimeUnit.SECONDS.toMillis(5));
    }

    public static BTraceDriver newDriver(int corePoolSize, long pollDelayMs, long timeoutMs) {
        return new BTraceDriver.Impl(corePoolSize, pollDelayMs, timeoutMs);
    }
    
    public static MeteringAware<BTraceSamplerFactoryProvider> defaultReporter() {
        return new StandardBTraceSamplerFactoryProvider();
    }
}

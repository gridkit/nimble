package org.gridkit.nimble.btrace;

import org.gridkit.nimble.driver.MeteringAware;

public class BTrace {
    public static BTraceDriver newDriver() {
        return newDriver(2);
    }

    public static BTraceDriver newDriver(int corePoolSize) {
        return new BTraceDriver.Impl(corePoolSize);
    }
    
    public static MeteringAware<BTraceSamplerFactoryProvider> defaultReporter() {
        return new StandardBTraceSamplerFactoryProvider();
    }
}

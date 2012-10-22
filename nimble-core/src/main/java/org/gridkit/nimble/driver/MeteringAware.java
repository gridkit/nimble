package org.gridkit.nimble.driver;

public interface MeteringAware<S> {
    S attach(MeteringDriver metering);
}

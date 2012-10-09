package org.gridkit.nimble.sensor;

public interface Sensor<M> {
    M measure() throws InterruptedException;

    public static interface Reporter<M> {
        void report(M m);
    }
}

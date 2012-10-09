package org.gridkit.nimble.sensor;

@SuppressWarnings("serial")
public abstract class IntervalMeasureSensor<M, S> extends SigarHolder implements Sensor<M> {    
    private long measureIntervalMs;

    public IntervalMeasureSensor(long measureIntervalMs) {
        this.measureIntervalMs = measureIntervalMs;
    }

    private transient S leftState;

    @Override
    public M measure() throws InterruptedException {
        leftState = getState();
        
        Thread.sleep(measureIntervalMs);
            
        S rightState = getState();

        M result = getMeasure(leftState, rightState);
            
        leftState = rightState;
            
        return result;
    }

    protected abstract S getState();
    
    protected abstract M getMeasure(S leftState, S rightState);
}

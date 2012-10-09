package org.gridkit.nimble.sensor;

import java.util.concurrent.TimeUnit;

import org.gridkit.nimble.statistics.StatsOps;
import org.gridkit.nimble.util.Pair;

public class IntervalMeasure<S> {
    private long leftTsNs;
    private long rightTsNs;
    
    private S leftState;
    private S rightState;
    
    public IntervalMeasure() {}
    
    public IntervalMeasure(Pair<Long, S> left, Pair<Long, S> right) {
        this.leftTsNs = left.getA();
        this.rightTsNs = right.getA();
        
        this.leftState = left.getB();
        this.rightState = right.getB();
    }
    
    public double getInterval(TimeUnit unit) {
        return StatsOps.convert(rightTsNs - leftTsNs, TimeUnit.NANOSECONDS, unit);
    }

    public long getLeftTsNs() {
        return leftTsNs;
    }

    public void setLeftTsNs(long leftTsNs) {
        this.leftTsNs = leftTsNs;
    }

    public long getRightTsNs() {
        return rightTsNs;
    }

    public void setRightTsNs(long rightTsNs) {
        this.rightTsNs = rightTsNs;
    }

    public S getLeftState() {
        return leftState;
    }

    public void setLeftState(S leftState) {
        this.leftState = leftState;
    }

    public S getRightState() {
        return rightState;
    }

    public void setRightState(S rightState) {
        this.rightState = rightState;
    }
}

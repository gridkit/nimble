package org.gridkit.nimble.execution;

import java.util.concurrent.TimeUnit;

public class ExecConditions {
    public static ExecCondition duration(long duration, TimeUnit unit) {
        return null;
    }
    
    public static ExecCondition duration(long durationS) {
        return duration(durationS, TimeUnit.SECONDS);
    }
    
    public static ExecCondition iterations(long iterations) {
        return null;
    }

    public static ExecCondition infinity() {
        return new ExecCondition() {
            @Override
            public boolean satisfied() {
                return true;
            }
        };
    }
    
    public static ExecCondition never() {
        return null;
    }
    
    public static ExecCondition or(ExecCondition... condition) {
        return null;
    }

    public static ExecCondition and(ExecCondition... condition) {
        return null;
    }
}

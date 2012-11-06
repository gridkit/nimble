package org.gridkit.nimble.execution;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("serial")
public class ExecConditions {
    public static ExecCondition duration(long duration, TimeUnit unit) {
        return new DurationCondition(unit.toMillis(duration));
    }
    
    public static ExecCondition duration(long durationS) {
        return duration(durationS, TimeUnit.SECONDS);
    }
    
    public static ExecCondition iterations(long iterations) {
        return new IterationsCondition(iterations);
    }

    public static ExecCondition infinity() {
        return new ConstantCondition(true);
    }
    
    public static ExecCondition never() {
        return new ConstantCondition(false);
    }
    
    public static ExecCondition or(ExecCondition... conditions) {
        return new OrCondition(Arrays.asList(conditions));
    }

    public static ExecCondition and(ExecCondition... conditions) {
        return new AndCondition(Arrays.asList(conditions));
    }
    
    private static class DurationCondition implements ExecCondition, Serializable {
        private final long durationMs; 
        private volatile Long startTsMs = null;
        
        public DurationCondition(long durationMs) {
            this.durationMs = durationMs;
        }

        @Override
        public boolean satisfied() {
            if (startTsMs == null) {
                startTsMs = System.currentTimeMillis();
            }

            return (System.currentTimeMillis() - startTsMs) < durationMs;
        }
    }
    
    private static class IterationsCondition implements ExecCondition, Serializable {
        private final AtomicLong iterationsRemain;

        public IterationsCondition(long iterations) {
            if (iterations < 0) {
                throw new IllegalArgumentException("iterations < 0");
            }
            
            this.iterationsRemain = new AtomicLong(iterations);
        }

        @Override
        public boolean satisfied() {
            return iterationsRemain.getAndDecrement() > 0;
        }
    }
    
    private static class ConstantCondition implements ExecCondition, Serializable {
        private final boolean value;

        public ConstantCondition(boolean value) {
            this.value = value;
        }

        @Override
        public boolean satisfied() {
            return value;
        }
    }
    
    private static class AndCondition implements ExecCondition, Serializable {
        private final List<ExecCondition> conditions;

        public AndCondition(List<ExecCondition> conditions) {
            if (conditions.isEmpty()) {
                throw new IllegalArgumentException("conditions.isEmpty()");
            }
            this.conditions = conditions;
        }
        
        @Override
        public boolean satisfied() {
            for (ExecCondition condition : conditions) {
                if (!condition.satisfied()) {
                    return false;
                }
            }
            return true;
        }
    }
    
    private static class OrCondition implements ExecCondition, Serializable {
        private final List<ExecCondition> conditions;

        public OrCondition(List<ExecCondition> conditions) {
            if (conditions.isEmpty()) {
                throw new IllegalArgumentException("conditions.isEmpty()");
            }
            this.conditions = conditions;
        }
        
        @Override
        public boolean satisfied() {
            for (ExecCondition condition : conditions) {
                if (condition.satisfied()) {
                    return true;
                }
            }
            return false;
        }
    }
}

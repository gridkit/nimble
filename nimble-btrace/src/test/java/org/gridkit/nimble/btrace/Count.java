package org.gridkit.nimble.btrace;

import java.io.Serializable;
import java.util.concurrent.Callable;

public class Count {
    public static volatile int VALUE = 0;
    
    public static void tick(int value) {
        VALUE = value;
    }
    
    public static Callable<Void> newCounter(int from, int nTicks) {
        return new Counter(from, nTicks);
    }
    
    @SuppressWarnings("serial")
    private static class Counter implements Callable<Void>, Serializable {
        private final int from;
        private final int nTicks;

        public Counter(int from, int nTicks) {
            this.from = from;
            this.nTicks = nTicks;
        }

        @Override
        public Void call() throws Exception {
            for (int i = 0; i < nTicks; ++i) {
                Count.tick(from + i);
            }
            
            return null;
        }
    }
}

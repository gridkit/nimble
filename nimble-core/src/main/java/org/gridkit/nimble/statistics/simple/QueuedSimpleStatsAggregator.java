package org.gridkit.nimble.statistics.simple;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public class QueuedSimpleStatsAggregator implements SimpleStatsAggregator {
    private final AtomicReference<SimpleStats> result;
    private final Queue<SimpleStats> calcQueue;
    
    public QueuedSimpleStatsAggregator() {
        this.result = new AtomicReference<SimpleStats>(new SimpleStats());
        this.calcQueue = new ConcurrentLinkedQueue<SimpleStats>();
    }
    
    @Override
    public void add(SimpleStats stats) {
        calcQueue.add(stats);
    }

    @Override
    public SimpleStats calculate() {
        synchronized (result) {
            while (!calcQueue.isEmpty()) {
                result.set(SimpleStats.combine(result.get(), calcQueue.poll()));
            }
        }
        return result.get();
    }
}

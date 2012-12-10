package org.gridkit.nimble.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class CriticalSectionTest {
    @Test
    public void test() throws Exception {
        long durationNs = TimeUnit.SECONDS.toNanos(10);
        
        long sleepMs = 0;
        
        int nLocks = 1;
        int nThreadsPerLock = 25;
        
        CriticalSection cs = new CriticalSection();
        
        List<Callable<Void>> workers = new ArrayList<Callable<Void>>();

        for (int lock = 0; lock < nLocks; ++lock) {
            AtomicInteger data = new AtomicInteger(0);
            
            for (int thread = 0; thread < nThreadsPerLock; ++thread) {
                Worker worker = new Worker();
                
                worker.durationNs = durationNs;
                worker.cs = cs;
                worker.data = data;
                worker.lock = lock;
                worker.sleepMs = sleepMs;
                
                workers.add(worker);
            }
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(nLocks * nThreadsPerLock);
        
        System.err.println("Start");
        
        List<Future<Void>> results = executor.invokeAll(workers);
        
        for (Future<Void> result : results) {
            result.get();
        }
        
        System.err.println("Done");
    }
    
    private static class Worker implements Callable<Void> {
        long durationNs;
        CriticalSection cs;
        AtomicInteger data;
        int lock;
        long sleepMs;
        
        @Override
        public Void call() throws Exception {
            long startNs = System.nanoTime();
            
            while (System.nanoTime() - startNs < durationNs) {
                //System.err.println("qqq");
                cs.execute(lock, executor);
            }
            
            return null;
        }
        
        private Callable<Void> executor = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                int count = data.incrementAndGet();
                
                if (sleepMs > 0) {
                    Thread.sleep(sleepMs);
                }
                
                try {
                    if (count != 1) {
                        throw new RuntimeException("count = " + count);
                    }
                } finally {
                    data.decrementAndGet();
                }

                return null;
            }
        };
    }
    
}

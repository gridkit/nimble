package org.gridkit.nimble.execution;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.gridkit.nimble.driver.Activity;
import org.gridkit.nimble.util.NamedThreadFactory;
import org.gridkit.nimble.util.Pair;

public class Execution {
    public static ExecutionDriver newDriver() {
        return new ExecutionDriverImpl();
    }
        
    @SuppressWarnings("serial")
    private static class ExecutionDriverImpl implements ExecutionDriver, Serializable {
        @Override
        public ExecutionPool newExecutionPool(String name) {
            return Execution.newExecutionPool(name);
        }

        @Override
        public ExecutionPool newExecutionPool(String name, int nTasks) {
            return Execution.newExecutionPool(name, nTasks);
        }
    }
    
    public static ExecutionPool newExecutionPool(String name) {
        return new Pool(name);
    }
    
    public static ExecutionPool newExecutionPool(String name, int nTasks) {
        return new Pool(name, nTasks);
    }

    private interface Semaphore {
       void acquire() throws InterruptedException;
       void release();
    }
    
    private static class FakeSemaphore implements Semaphore {
        @Override
        public void acquire() throws InterruptedException {
        }

        @Override
        public void release() {
        }
    }
    
    private static class JavaSemaphore implements Semaphore {
        private final java.util.concurrent.Semaphore delegate;
        
        public JavaSemaphore(int permits) {
            delegate = new java.util.concurrent.Semaphore(permits, true);
        }

        @Override
        public void acquire() throws InterruptedException {
            delegate.acquire();
        }

        @Override
        public void release() {
            delegate.release();
        }
    }
    
    private static class Pool implements ExecutionPool {
        private final AtomicReference<Semaphore> semaphore = new AtomicReference<Semaphore>();
        private final ExecutorService executor;

        public Pool(String name) {
            this.executor = Executors.newCachedThreadPool(
                new NamedThreadFactory(name, true, Thread.NORM_PRIORITY)
            );
            unlimitConcurrentTasks();
        }
        
        public Pool(String name, int nTasks) {
             this(name);
             concurrentTasks(nTasks);
        }

        @Override
        public Activity exec(ExecConfig config) {
            config.getCondition().init();
            return new Handle(config, this);
        }

        @Override
        public void concurrentTasks(int nTasks) {
            if (nTasks < 1) {
                throw new IllegalArgumentException("nTasks < 1");
            }
            semaphore.set(new JavaSemaphore(nTasks));
        }

        @Override
        public void unlimitConcurrentTasks() {
            semaphore.set(new FakeSemaphore());
        }

        public Pair<Worker, Future<Void>> submit(Task task, ExecConfig config, CountDownLatch latch) {
            Worker worker = new Worker(task, config, semaphore, latch);
            return Pair.newPair(worker, executor.submit(worker));
        }
        
        @Override
        public void shutdown() {
            executor.shutdown();
        }
    }
    
    // TODO finish join after first Exception
    private static class Handle implements Activity {      
        private final List<Pair<Worker, Future<Void>>> context = new ArrayList<Pair<Worker,Future<Void>>>();
        private final CountDownLatch latch;
        
        protected boolean stoppped = false;
        
        public Handle(ExecConfig config, Pool pool) {
            this.latch = new CountDownLatch(config.getTasks().size());
            
            for (Task task : config.getTasks()) {
                context.add(pool.submit(task, config, latch));
            }
        }

        @Override
        public void join() {
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            
            for (Pair<Worker, Future<Void>> entry : context) {
                Future<Void> future = entry.getB();
                
                try {
                    if (future.isDone() && !future.isCancelled()) {
                        future.get();
                    }
                } catch (ExecutionException e) {
                   throw new RuntimeException(e.getCause());
                } catch (Exception e) {
                   throw new RuntimeException(e);
                }
            }
        }
        
        @Override
        public synchronized void stop() {
            if (stoppped) {
                return;
            }
            
            for (int i = 0; i < context.size(); ++i) {
                Worker worker = context.get(i).getA();
                Future<Void> future = context.get(i).getB();
                
                try {
                    worker.cancel(future);
                } catch (Exception e) {
                    // ignored
                }
            }
            
            stoppped = true;
        }
    }
    
    private static class Worker implements Callable<Void> {        
        private final ExecConfig config;
        private final Task task;
        private final AtomicReference<Semaphore> semaphore;
        private final CountDownLatch latch;
        
        private boolean done = false;
        
        private Object lock = new Object();
        private boolean canceled = false;
        private Thread thread = null;
        
        public Worker(Task task, ExecConfig config, AtomicReference<Semaphore> semaphore, CountDownLatch latch) {
            this.config = config;
            this.task = task;
            this.latch = latch;
            this.semaphore = semaphore;
        }

        @Override
        public Void call() throws Exception {
            try {
                run();
            } catch (Exception e) {
                if (!done) {
                    throw e;
                }
            }
            return null;
        }
        
        public void run() throws Exception {
            while (true) {
                if (!done && !config.getCondition().satisfied()) {
                    latch.countDown();
                    done = true;
                    if (!config.isManualShutdown()) {
                        return;
                    }
                }

                Semaphore semaphore = this.semaphore.get();
                semaphore.acquire();

                try {
                    config.getBarrier().pass();
                    try {
                        synchronized (lock) {
                            if (canceled) {
                                return;
                            }
                            thread = Thread.currentThread();
                        }
                        task.run();
                    } finally {
                        Thread.interrupted(); // cleaning up thread interrupted status
                        synchronized (lock) {
                            thread = null;
                            if (canceled) {
                                return;
                            }
                        }
                    }
                } finally {
                    semaphore.release();
                }
            }
        }
        
        public void cancel(final Future<Void> future) throws Exception {
            synchronized (lock) {
                canceled = true;
                
                if (thread != null) {
                    task.cancel(thread);
                } else {
                    future.cancel(true);
                }
            }
        }
    }
}

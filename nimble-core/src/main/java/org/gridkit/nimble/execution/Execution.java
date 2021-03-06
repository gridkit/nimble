package org.gridkit.nimble.execution;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import org.gridkit.nimble.driver.Activity;
import org.gridkit.nimble.util.NamedThreadFactory;

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
        public DynamicSemaphore newDynamicSemaphore(int nPermits) {
            return Execution.newDynamicSemaphore(nPermits);
        }
    }
    
    public static ExecutionPool newExecutionPool(String name) {
        return new Pool(name);
    }
    
    public static DynamicSemaphore newDynamicSemaphore() {
        return new VaryingSemaphore();
    }
    
    public static DynamicSemaphore newDynamicSemaphore(int nPermits) {
        return new VaryingSemaphore(nPermits);
    }
        
    public static class CompositeActivity implements Activity {
        private final Collection<? extends Activity> delegates;
        
        public CompositeActivity(Collection<? extends Activity> delegates) {
            this.delegates = delegates;
        }

        @Override
        public void join() {
            for (Activity delegate : delegates) {
                delegate.join();
            }
        }
        
        @Override
        public void stop() {
            for (Activity delegate : delegates) {
                delegate.stop();
            }
        }
    }
    
    private static class Pool implements ExecutionPool {
        private final ExecutorService executor;

        public Pool(String name) {
            this.executor = Executors.newCachedThreadPool(
                new NamedThreadFactory(name, true, Thread.NORM_PRIORITY)
            );
        }

        @Override
        public Activity exec(ExecConfig config) {
            List<Activity> activities = new ArrayList<Activity>();
            
            config.getCondition().init();
            
            for (Task task : config.getTasks()) {
                CountDownLatch latch = new CountDownLatch(1);
                Worker worker = new Worker(task, config, latch);
                Future<Void> future = executor.submit(worker);
                
                activities.add(new Handle(worker, future, latch));
            }
            
            return new CompositeActivity(activities);
        }
        
        @Override
        public void stop() {
            executor.shutdown();
        }
    }
    
    private static class Handle implements Activity {  
        private final Worker worker;
        private final Future<Void> future;
        private final CountDownLatch latch;
        
        protected boolean stoppped = false;
        
        public Handle(Worker worker, Future<Void> future, CountDownLatch latch) {
            this.worker = worker;
            this.future = future;
            this.latch = latch;
        }

        @Override
        public void join() {
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
         
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
        
        @Override
        public synchronized void stop() {
            if (stoppped) {
                return;
            }
            
            try {
                worker.cancel(future);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            
            stoppped = true;
        }
    }
    
    private static class Worker implements Callable<Void> {        
        private final ExecConfig config;
        private final Task task;
        private final CountDownLatch latch;
        
        private boolean done = false;
        
        private Object lock = new Object();
        private boolean canceled = false;
        private Thread thread = null;
        
        public Worker(Task task, ExecConfig config, CountDownLatch latch) {
            this.config = config;
            this.task = task;
            this.latch = latch;
        }

        @Override
        public Void call() throws Exception {
            try {
                run();
            } catch (Exception e) {
                if (!done) {
                    latch.countDown();
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
                    if (!config.isManualStop()) {
                        return;
                    }
                }

                try {
                    synchronized (lock) {
                        if (canceled) {
                            return;
                        }
                        thread = Thread.currentThread();
                    }
                    task.run();
                } finally {
                    synchronized (lock) {
                        Thread.interrupted(); // clearing up thread's interrupted status
                        thread = null;
                        if (canceled) {
                            return;
                        }
                    }
                }
            }
        }
        
        public void cancel(final Future<Void> future) throws Exception {
            synchronized (lock) {
                canceled = true;
                
                if (thread != null) {
                    future.cancel(false);
                    task.cancel(thread);
                } else {
                    future.cancel(true);
                }
            }
        }
    }
    
    // TODO add support multiple acquires within one thread
    private static class VaryingSemaphore implements DynamicSemaphore {
        private final AtomicReference<Semaphore> globalSemaphore = new AtomicReference<Semaphore>(null);
        private final ThreadLocal<Semaphore> localSemaphore = new ThreadLocal<Semaphore>();
        
        public VaryingSemaphore() {
        }
        
        public VaryingSemaphore(int nPermits) {
            permits(nPermits);
        }

        @Override
        public void acquire() throws InterruptedException {
            Semaphore semaphore = globalSemaphore.get();
            if (semaphore != null) {
                semaphore.acquire();
                localSemaphore.set(semaphore);
            }
        }

        @Override
        public void release() {
            Semaphore semaphore = localSemaphore.get();
            if (semaphore != null) {
                semaphore.release();
                localSemaphore.set(null);
            }
        }

        @Override
        public void permits(int nPermits) {
            if (nPermits < 1) {
                throw new IllegalArgumentException("nPermits < 1");
            }
            globalSemaphore.set(new Semaphore(nPermits));
        }

        @Override
        public void disable() {
            globalSemaphore.set(null);
        }
    }
}

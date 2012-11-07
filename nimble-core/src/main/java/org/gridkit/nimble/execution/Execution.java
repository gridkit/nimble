package org.gridkit.nimble.execution;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
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
        public ExecutionPool newExecutionPool(String name, int threads) {
            return Execution.newExecutionPool(name, threads);
        }
    }
    
    public static ExecutionPool newExecutionPool(String name) {
        return new Pool(name);
    }
    
    public static ExecutionPool newExecutionPool(String name, int threads) {
        return new Pool(name, threads);
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
                new NamedThreadFactory("ExecDriver", true, Thread.NORM_PRIORITY)
            );
            setThreadPerTask();
        }
        
        public Pool(String name, int threads) {
             this(name);
             setThreadsNumber(threads);
        }

        @Override
        public Activity exec(ExecConfig config) {
            config.getCondition().init();
            return new Handle(config, this);
        }

        @Override
        public void setThreadsNumber(int threads) {
            if (threads < 1) {
                throw new IllegalArgumentException("threads < 1");
            }
            semaphore.set(new JavaSemaphore(threads));
        }

        @Override
        public void setThreadPerTask() {
            semaphore.set(new FakeSemaphore());
        }

        public Pair<Worker, Future<Void>> submit(Task task, ExecConfig config, BlockingQueue<Object> resultQueue) {
            Worker worker = new Worker(config, resultQueue, task, semaphore);
            return Pair.newPair(worker, executor.submit(worker));
        }
        
        @Override
        public void shutdown() {
            executor.shutdown();
        }
    }
    
    // TODO handle multiple joins and stops
    private static class Handle implements Activity {        
        private final ExecConfig config;
        private final BlockingQueue<Object> resultQueue = new LinkedBlockingQueue<Object>();
        private final List<Pair<Worker, Future<Void>>> context = new ArrayList<Pair<Worker,Future<Void>>>();
        
        private final Object joinLock = new Object();
        private final Object shutdownLock = new Object();
        
        protected Object joinResult = null;
        protected boolean shutdown = false;
        
        public Handle(ExecConfig config, Pool pool) {
            this.config = config;
            for (Task task : config.getTasks()) {
                context.add(pool.submit(task, config, resultQueue));
            }
        }

        @Override
        public void join() {
            synchronized (joinLock) {
                if (joinResult != null) {
                    if (joinResult instanceof Exception) {
                        throw new RuntimeException((Exception)joinResult);
                    } else {
                        return;
                    }
                }
                
                int resultsReceived = 0;
                int resultsToJoin = config.getTasks().size();
                
                while (resultsReceived < resultsToJoin) {
                    try {
                        Object result = resultQueue.take();
                        
                        resultsReceived += 1;
                        
                        if (result instanceof Exception) {
                            throw (Exception)result;
                        }
                    } catch (Exception e) {
                        joinResult = e;
                        shutdown(e);
                    }
                }
                
                joinResult = new Object();
            }
        }
        
        @Override
        public void stop() {
            try {
                shutdown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        private void shutdown(Exception e1) {
            try {
                shutdown();
            } catch (Exception e2) {
                // ignored
            }
            throw new RuntimeException(e1);
        }
        
        private void shutdown() throws Exception {
            synchronized (shutdownLock) {
                if (shutdown) {
                    return;
                }
                
                Exception exception = null;
                
                for (int i = 0; i < context.size(); ++i) {
                    Worker worker = context.get(i).getA();
                    Future<Void> future = context.get(i).getB();
                    
                    try {
                        worker.cancel(future);
                    } catch (Exception e) {
                        if (exception == null) {
                            exception = e;
                        }
                    }
                }
                
                shutdown = true;
                
                if (exception != null) {
                    throw exception;
                }
            }
        }
    }
    
    private static class Worker implements Callable<Void> {        
        private final ExecConfig config;
        private final BlockingQueue<Object> resultQueue;
        private final Task task;
        private final AtomicReference<Semaphore> semaphore;

        private boolean continuous = false;
        private boolean resultSent = false;

        private Object lock = new Object();
        private boolean canceled = false;
        private boolean excecuted = false;
        
        public Worker(ExecConfig config, BlockingQueue<Object> resultQueue, Task task, AtomicReference<Semaphore> semaphore) {
            this.config = config;
            this.resultQueue = resultQueue;
            this.task = task;
            this.semaphore = semaphore;
        }

        public void callInternal() throws Exception  {
            while (true) {
                if (continuous || !config.getCondition().satisfied()) {
                    success();
                    if (!config.isContinuous()) {
                        return;
                    } else {
                        continuous = true;
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
                            excecuted = true;
                        }
                        task.run();
                    } finally {
                        synchronized (lock) {
                            if (canceled) {
                                return;
                            }
                            excecuted = false;
                        }
                    }
                } finally {
                    semaphore.release();
                }
            }
        }
        
        public Void call() throws Exception {
            try {
                callInternal();
                success();
            } catch (Exception e) {
                if (!isCanceled()) {
                    error(e);
                } else {
                    success();
                }
            }
            return null;
        }
        
        private void success() {
            result(new Object());
        }
        
        private void error(Exception e) {
            result(e);
        }
        
        private void result(Object result) {
            if (!resultSent) {
                synchronized (lock) {
                    resultQueue.add(result);
                }
                resultSent = true;
            }
        }

        private boolean isCanceled() {
            synchronized (lock) {
                return canceled;
            }
        }
        
        public void cancel(final Future<Void> future) throws Exception {
            synchronized (lock) {
                canceled = true;
                
                task.cancel(new Task.Interruptible() {
                    @Override
                    public void interrupt() {
                        synchronized (lock) {
                            if (excecuted) {
                                future.cancel(true);
                            }
                        }
                    }
                });
                
                synchronized (lock) {
                    if (!excecuted) {
                        future.cancel(true);
                    }
                }
            }
        }
    }
}

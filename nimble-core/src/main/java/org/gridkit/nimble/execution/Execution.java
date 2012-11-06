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

import org.gridkit.nimble.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Execution {
    public static ExecutionDriver newDriver() {
        return new ExecutionDriverImpl();
    }
        
    @SuppressWarnings("serial")
    private static class ExecutionDriverImpl implements ExecutionDriver, Serializable {
        private transient ExecutorService executor;
        
        @Override
        public ExecHandle newExecution(ExecConfig config) {
            return newExecHandle(config, getExecutor()); 
        }
        
        @Override
        public void shutdown() {
            getExecutor().shutdown();
        }
        
        private synchronized ExecutorService getExecutor() {
            if (executor == null) {
                executor = Executors.newCachedThreadPool(
                    new NamedThreadFactory("ExecDriver", true, Thread.NORM_PRIORITY)
                );
            }
            return executor;
        }
    }
    
    public static ExecHandle newExecHandle(ExecConfig config, ExecutorService executor) {
        return new Handle(config, executor);
    }

    private static class Context {
        protected List<Worker> workers;
        protected List<Future<Void>> futures;
        protected boolean shutdown = false;
        
        protected Context() {
            this.workers = new ArrayList<Worker>();
            this.futures = new ArrayList<Future<Void>>();
        }
        
        protected synchronized void submit(Worker worker, ExecutorService executor) {
            if (shutdown) {
                throw new IllegalStateException();
            }
            
            Future<Void> future = executor.submit(worker);
            
            workers.add(worker);
            futures.add(future);
        }
        
        protected synchronized void shutdown() throws Exception {
            if (!shutdown) {
                Exception exception = null;
                
                for (int i = 0; i < workers.size(); ++i) {
                    Worker worker = workers.get(i);
                    Future<Void> future = futures.get(i);
                    
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
        
    // TODO protect from incorrect call sequence of start, join, stop
    private static class Handle implements ExecHandle {        
        private Context context;
        private Context prevContext;
        private ExecConfig config;
        private ExecutorService executor;
          
        private BlockingQueue<Object> resultQueue = new LinkedBlockingQueue<Object>();
        
        public Handle(Context prevContext, ExecConfig config, ExecutorService executor) {
            this.context = new Context();
            this.prevContext = prevContext;
            this.config = config;
            this.executor = executor;
        }
        
        public Handle(ExecConfig config, ExecutorService executor) {
            this(new Context(), config, executor);
        }
        
        @Override
        public void start() {
            try {
                startInternal();
                resultQueue.add(new Object());
            } catch (Exception e) {
                resultQueue.add(e);
                shutdown(e);
            }
        }
        
        // TODO add incremental start
        private void startInternal() throws Exception {
            prevContext.shutdown();

            for (int i = 0; i < config.getThreads(); ++i) {
                submitWorker();
            }
        }
        
        private void submitWorker() {
            Worker worker = new Worker(config, resultQueue);
            context.submit(worker, executor);
        }

        @Override
        public void join() {
            int resultsReceived = 0;
            int resultsToJoin = config.getThreads() + 1;
            
            while (resultsReceived < resultsToJoin) {
                try {
                    Object result = resultQueue.take();
                    
                    resultsReceived += 1;
                    
                    if (result instanceof Exception) {
                        throw (Exception)result;
                    }
                } catch (Exception e) {
                    shutdown(e);
                }
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
        
        private synchronized void shutdown() throws Exception {
            try {
                context.shutdown();
            } finally {
                try {
                    prevContext.shutdown();
                } finally {
                    executor.shutdown();
                }
            }
        }

        @Override
        public ExecHandle proceed(ExecConfig config) {
            return new Handle(context, config, executor);
        }
    }
    
    private static class Worker implements Callable<Void> {
        private static final Logger log = LoggerFactory.getLogger(Worker.class);
        
        private final ExecConfig config;
        
        private final BlockingQueue<Object> resultQueue;
        private boolean resultSent = false;
        
        private Object lock = new Object();
        private Task task = null;
        private boolean canceled = false;
        
        public Worker(ExecConfig config, BlockingQueue<Object> resultQueue) {
            this.config = config;
            this.resultQueue = resultQueue;
        }

        public void callInternal() throws Exception  {
            while (true) {
                if (!resultSent && !config.getCondition().satisfied()) {                    
                    resultQueue.put(new Object());
                    resultSent = true;
                    
                    if (!config.isContinuous()) {
                        return;
                    }
                }
                
                config.getBarrier().pass();
                
                synchronized (lock) {
                    if (canceled) {
                        return;
                    }
                    task = config.getProvider().nextTask();
                }
                                
                if (task == null) {
                    return;
                }
                
                try {
                    task.run();
                } finally {
                    synchronized (lock) {
                        task = null;
                        if (canceled) {
                            return;
                        }
                    }
                }
            }
        }
        
        public Void call() throws Exception {
            try {
                callInternal();
            } catch (Exception e) {
                if (!resultSent) {
                    if (e instanceof InterruptedException) {
                        resultQueue.put(new Object());
                    } else {
                        resultQueue.put(e);
                    }
                } else {
                    if (!(e instanceof InterruptedException)) {
                        log.warn("Exception caught after task was reported as successful", e);
                    }
                }
            }
            return null;
        }
        
        public void cancel(final Future<Void> future) throws Exception {
            synchronized (lock) {
                if (!canceled) {
                    canceled = true;
                    
                    if (task != null) {
                        task.cancel(new Task.Interruptible() {
                            @Override
                            public void interrupt() {
                                future.cancel(true);
                            }
                        });
                    } else {
                        future.cancel(true);
                    }
                }
            }
        }
    }
}

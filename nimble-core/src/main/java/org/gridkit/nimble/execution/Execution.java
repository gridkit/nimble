package org.gridkit.nimble.execution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.gridkit.util.concurrent.Barriers;
import org.gridkit.util.concurrent.BlockingBarrier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Execution {
    public static ExecutionDriver newDriver() {
        return new ExecutionDriver() {
            @Override
            public ExecBuilder newExecBuilder() {
                return newExecBuilder();
            }
        };
    }
    
    public static ExecBuilder newExecBuilder() {
        Context context = new Context();
        
        context.executor = Executors.newCachedThreadPool();
        context.workers = Collections.emptyList();
        context.futures = Collections.emptyList();
        
        return new Builder(context);
    }

    private static class Context {
        // variables passed between sequential executions
        protected ExecutorService executor;
        protected List<Worker> workers;
        protected List<Future<Void>> futures;
        
        // per execution variables
        protected TaskProvider provider;
        protected ExecCondition condition = ExecConditions.infinity();
        protected BlockingBarrier barrier = Barriers.openBarrier();
        protected boolean continuous = true;
        protected int threads = 1;

        protected Builder nextBuilder() {
            Context context = new Context();
            
            context.executor = executor;
            context.workers = workers;
            context.futures = futures;
            
            return new Builder(context);
        }
        
        protected ExecHandle nextExecHandle() {            
            Context context = new Context();
            
            context.executor = executor;
            context.workers = new ArrayList<Worker>();
            context.futures = new ArrayList<Future<Void>>();

            return new Handle(context, this);
        }
        
        protected boolean valid() {
            return (provider != null) && (condition != null) && (barrier != null) && (threads > 0);
        }
    }
    
    private static class Builder implements ExecBuilder {
        private Context context;

        public Builder(Context context) {
            this.context = context;
        }

        @Override
        public synchronized ExecBuilder provider(TaskProvider provider) {
            context.provider = provider;
            return this;
        }

        @Override
        public synchronized ExecBuilder condition(ExecCondition condition) {
            context.condition = condition;
            return this;
        }

        @Override
        public synchronized ExecBuilder barrier(BlockingBarrier barrier) {
            context.barrier = barrier;
            return this;
        }

        @Override
        public synchronized ExecBuilder threads(int threads) {
            context.threads = threads;
            return this;
        }
        
        @Override
        public synchronized ExecBuilder continuous(boolean continuous) {
            context.continuous = continuous;
            return this;
        }

        @Override
        public synchronized ExecHandle build() {
            if (!context.valid()) {
                throw new IllegalStateException("Built context is invalid");
            }
            ExecHandle result = context.nextExecHandle();
            context = null; // invalidate builder
            return result;
        }
    }
    
    // TODO protect from incorrect call sequence of start, join, stop
    private static class Handle implements ExecHandle {        
        private Context context;
        private Context prevContext;
          
        private BlockingQueue<Object> resultQueue = new LinkedBlockingQueue<Object>();
        private int resultsToJoin;
        
        public Handle(Context context, Context prevContext) {
            this.context = context;
            this.prevContext = prevContext;
            this.resultsToJoin = context.threads + 1;
        }
        
        @Override
        public synchronized ExecHandle start() {
            try {
                startInternal();
            } catch (Exception e) {
                shutdown(e);
            }
            return this;
        }
        
        private void startInternal() throws Exception {
            int startFirst = Math.max(0, context.threads - prevContext.workers.size());
            
            while (startFirst-- > 0) {
                startWorker();
            }
            
            for (int i = 0; i < prevContext.workers.size(); ++i) {
                Worker worker = prevContext.workers.get(i);
                Future<Void> future = prevContext.futures.get(i);

                if (!worker.isCanceled()) {
                    worker.cancel(future);
                }
                
                startWorker();
            }
        }
        
        private void startWorker() {
            Worker worker = new Worker(context, resultQueue);
            Future<Void> future = context.executor.submit(worker);
            
            context.workers.add(worker);
            context.futures.add(future);
        }

        @Override
        public ExecBuilder join() {
            int resultsReceived = 0;
            
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
            
            return context.nextBuilder();
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
            Exception e1 = shutdown(prevContext);
            Exception e2 = shutdown(context);
            
            // invalidate handle
            context = null;
            prevContext = null;
            
            context.executor.shutdown();
            
            if (e1 != null) {
                throw e1;
            } else if (e2 != null) {
                throw e2;
            }
        }
        
        private static Exception shutdown(Context context) {
            Exception result = null;
            
            for (int i = 0; i < context.workers.size(); ++i) {
                Worker worker = context.workers.get(i);
                Future<Void> future = context.futures.get(i);
                
                if (!worker.isCanceled()) {
                    try {
                        worker.cancel(future);
                    } catch (Exception e) {
                        if (result == null) {
                            result = e;
                        }
                    }
                }
            }
            
            return result;
        }
    }
    
    private static class Worker implements Callable<Void> {
        private static final Logger log = LoggerFactory.getLogger(Worker.class);
        
        private final Context context;
        
        private final BlockingQueue<Object> resultQueue;
        private boolean resultSent = false;
        
        private Object lock = new Object();
        private Task task = null;
        private boolean canceled = false;
        
        public Worker(Context context, BlockingQueue<Object> resultQueue) {
            this.context = context;
            this.resultQueue = resultQueue;
        }

        public void callInternal() throws Exception  {
            while (true) {
                if (!resultSent && !context.condition.satisfied()) {                    
                    resultQueue.put(new Object());
                    resultSent = true;
                    
                    if (!context.continuous) {
                        return;
                    }
                }
                
                context.barrier.pass();
                
                synchronized (lock) {
                    if (canceled) {
                        return;
                    }
                    task = context.provider.nextTask();
                }
                                
                if (task == null) {
                    return;
                }
                
                try {
                    task.execute();
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
        
        public void cancel(Future<Void> future) throws Exception {
            synchronized (lock) {
                canceled = true;
                
                if (task != null) {
                    task.cancel(future);
                } else {
                    task.cancel(null);
                    future.cancel(true);
                }
            }
        }
        
        public boolean isCanceled() {
            synchronized (lock) {
                return canceled;
            }
        }
    }
}

package org.gridkit.nimble.execution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.gridkit.util.concurrent.Barriers;
import org.gridkit.util.concurrent.BlockingBarrier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecConfigBuilder {
    private List<Task> tasks = Collections.emptyList();
    private ExecCondition condition = ExecConditions.infinity();
    private BlockingBarrier barrier = null;
    private Semaphore semaphore = null;
    private Integer splits = null;
    private boolean manualStop = false;
    private boolean runEachTaskOnce = false;
    private boolean ignoreErrors = false;
    private boolean logErrors = false;
    private boolean interruptOnCancel = false;
    private boolean valid = true;
    
    public ExecConfigBuilder tasks(Collection<Task> tasks) {
        this.tasks = new ArrayList<Task>(tasks);
        return this;
    }
    
    public ExecConfigBuilder runnables(Collection<Runnable> runnables) {
        this.tasks = new ArrayList<Task>(tasks.size());
        
        for (Runnable task : runnables) {
            this.tasks.add(new RunnableAdapter(task));
        }
        
        return this;
    }
    
    public ExecConfigBuilder callables(Collection<Callable<?>> callables) {
        this.tasks = new ArrayList<Task>(tasks.size());
        
        for (Callable<?> task : callables) {
            this.tasks.add(new CallableAdapter(task));
        }
        
        return this;
    }
    
    public ExecConfigBuilder tasks(Task... tasks) {
        return tasks(Arrays.asList(tasks));
    }
    
    public ExecConfigBuilder runnables(Runnable... runnables) {
        return runnables(Arrays.asList(runnables));
    }
    
    public ExecConfigBuilder callables(Callable<?>... callables) {
        return callables(Arrays.asList(callables));
    }
    
    public ExecConfigBuilder tasks(Task task, int nCopies) {
        return tasks(Collections.nCopies(nCopies, task));
    }
    
    public ExecConfigBuilder runnables(Runnable runnable, int nCopies) {
        return runnables(Collections.nCopies(nCopies, runnable));
    }
    
    public ExecConfigBuilder callables(Callable<?> callable, int nCopies) {
        return callables(Collections.<Callable<?>>nCopies(nCopies, callable));
    }
    
    public ExecConfigBuilder condition(ExecCondition condition) {
        this.condition = condition;
        return this;
    }
    
    public ExecConfigBuilder barrier(BlockingBarrier barrier){
        this.barrier = barrier;
        return this;
    }
    
    public ExecConfigBuilder rate(double opsPerSecond) {
        return barrier(Barriers.speedLimit(opsPerSecond));
    }
    
    public ExecConfigBuilder rate(double ops, TimeUnit unit) {
        double unitNs = unit.toNanos(1);
        double secondNs = TimeUnit.SECONDS.toNanos(1);
        
        return rate(ops * (secondNs / unitNs));
    }
    
    public ExecConfigBuilder semaphore(Semaphore semaphore) {
        this.semaphore = semaphore;
        return this;
    }
    
    public ExecConfigBuilder manualStop() {
        this.manualStop = true;
        return this;
    }
    
    public ExecConfigBuilder runEachTaskOnce() {
        this.runEachTaskOnce = true;
        return this;
    }
    
    public ExecConfigBuilder ignoreErrors() {
        this.ignoreErrors = true;
        return this;
    }

    public ExecConfigBuilder logErrors() {
        this.logErrors = true;
        return this;
    }
    
    public ExecConfigBuilder interruptOnCancel() {
        this.interruptOnCancel = true;
        return this;
    }
    
    public ExecConfigBuilder duration(long duration, TimeUnit unit) {
        return condition(ExecConditions.duration(duration, unit));
    }
    
    public ExecConfigBuilder duration(long durationS) {
        return condition(ExecConditions.duration(durationS));
    }
    
    public ExecConfigBuilder iterations(long iterations) {
        return condition(ExecConditions.iterations(iterations));
    }
    
    public ExecConfigBuilder split(int splits) {
        if (splits < 1) {
            throw new IllegalArgumentException("splits < 1");
        }
        this.splits = splits;
        return this;
    }
    
    private boolean valid() {
        return valid && (tasks != null) && (condition != null);
    }
    
    public ExecConfig build() {
        if (!valid()) {
            throw new IllegalStateException("ExecConfigBuilder state is invalid");
        }
        
        InternalExecConfig result = new InternalExecConfig();
        
        result.condition = runEachTaskOnce ? ExecConditions.once(tasks) : condition;
        result.manualShutdown = manualStop;
        
        ListIterator<Task> iter = tasks.listIterator();
        while (iter.hasNext()) {
            Task task = iter.next();
            
            if (semaphore != null) {
                task = new SemaphoreTask(task, semaphore);
            }
            
            if (barrier != null) {
                task = new BarrierTask(task, barrier);
            }
            
            if (logErrors) {
                task = new LoggingTask(task);
            }
            
            if (interruptOnCancel) {
                task = new InterruptingTask(task);
            }

            if (ignoreErrors) {
                task = new SafeTask(task);
            }
            
            iter.set(task);
        }
        
        if (splits != null && tasks.size() > splits) {
            BlockingQueue<Task> queue = new ArrayBlockingQueue<Task>(tasks.size(), true, tasks);
            
            tasks = new ArrayList<Task>(splits);
            for (int t = 0; t < splits; ++t) {
                tasks.add(new SplitTask(queue));
            }
        }
        
        result.tasks = tasks;

        valid = false;
        
        return result;
    }
    
    private static class InternalExecConfig implements ExecConfig {
        protected Collection<Task> tasks;
        protected ExecCondition condition;
        protected boolean manualShutdown;
        
        @Override
        public Collection<Task> getTasks() {
            return tasks;
        }

        @Override
        public ExecCondition getCondition() {
            return condition;
        }

        @Override
        public boolean isManualStop() {
            return manualShutdown;
        }
    }
    
    private interface DelegatingTask extends Task {
        Object getDelegate();
    }
    
    private static Object getTaskRunner(Object task) {
        if (task instanceof DelegatingTask) {
            return getTaskRunner(((DelegatingTask)task).getDelegate());
        } else {
            return task;
        }
    }
    
    private static class SafeTask implements DelegatingTask {
        private final Task delegate;
        
        public SafeTask(Task delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() throws Exception {
            try {
                delegate.run();
            } catch (Exception e) {
                // ignored
            }
        }

        @Override
        public void cancel(Thread thread) throws Exception {
            try {
                delegate.cancel(thread);
            } catch (Exception e) {
                // ignored
            }
        }

        @Override
        public Object getDelegate() {
            return delegate;
        }
    }
    
    private static class LoggingTask implements DelegatingTask {
        private final Task delegate;
        private final Object runner;
        private final Logger log;
        
        public LoggingTask(Task delegate) {
            this.delegate = delegate;
            this.runner = getTaskRunner(delegate);
            this.log = LoggerFactory.getLogger(runner.getClass());
        }

        @Override
        public void run() throws Exception {
            try {
                delegate.run();
            } catch (Exception e) {
                log.error("Exception while running " + runner, e);
                throw e;
            }
        }

        @Override
        public void cancel(Thread thread) throws Exception {
            try {
                delegate.cancel(thread);
            } catch (Exception e) {
                log.error("Exception while canceling " + runner, e);
                throw e;
            }
        }

        @Override
        public Object getDelegate() {
            return delegate;
        }
    }
    
    private static class RunnableAdapter implements DelegatingTask  {
        private final Runnable delegate;
        
        public RunnableAdapter(Runnable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() throws Exception {
            delegate.run();
        }
        
        @Override
        public void cancel(Thread thread) throws Exception {            
        }
        
        @Override
        public Object getDelegate() {
            return delegate;
        }
    }
    
    private static class CallableAdapter implements DelegatingTask {
        private final Callable<?> delegate;
        
        public CallableAdapter(Callable<?> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() throws Exception {
            delegate.call();
        }
        
        @Override
        public void cancel(Thread thread) throws Exception {            
        }
        
        @Override
        public Object getDelegate() {
            return delegate;
        }
    }
    
    private static class SplitTask implements Task, DelegatingTask {
        private final BlockingQueue<Task> tasks;
        private volatile Task curTask;
        
        public SplitTask(BlockingQueue<Task> tasks) {
            this.tasks = tasks;
        }

        @Override
        public void run() throws Exception {
            try {
                curTask = tasks.take();
                curTask.run();
            } finally {
                if (curTask != null) {
                    tasks.add(curTask);
                }
            }
        }

        @Override
        public void cancel(Thread thread) throws Exception {
            curTask.cancel(thread);
        }
        
        @Override
        public Object getDelegate() {
            return curTask;
        }
    }
    
    private static class InterruptingTask implements DelegatingTask {
        private final Task delegate;
        
        public InterruptingTask(Task delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() throws Exception {
            delegate.run();
        }

        @Override
        public void cancel(Thread thread) throws Exception {
            try {
                delegate.cancel(thread);
            } finally {
                thread.interrupt();
            }
        }

        @Override
        public Object getDelegate() {
            return delegate;
        }
    }
    
    private static class BarrierTask implements DelegatingTask {
        private final Task delegate;
        private final BlockingBarrier barrier;
        
        public BarrierTask(Task delegate, BlockingBarrier barrier) {
            this.delegate = delegate;
            this.barrier = barrier;
        }

        @Override
        public void run() throws Exception {
            barrier.pass();
            delegate.run();
        }

        @Override
        public void cancel(Thread thread) throws Exception {
            delegate.cancel(thread);
        }

        @Override
        public Object getDelegate() {
            return delegate;
        }
    }
    
    private static class SemaphoreTask implements DelegatingTask {
        private final Task delegate;
        private final Semaphore semaphore;
        
        public SemaphoreTask(Task delegate, Semaphore semaphore) {
            this.delegate = delegate;
            this.semaphore = semaphore;
        }

        @Override
        public void run() throws Exception {
            semaphore.acquire();
            try {
                delegate.run();
            } finally {
                semaphore.release();
            }
        }

        @Override
        public void cancel(Thread thread) throws Exception {
            delegate.cancel(thread);
        }

        @Override
        public Object getDelegate() {
            return delegate;
        }
    }
}

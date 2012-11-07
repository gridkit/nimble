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
    private BlockingBarrier barrier = Barriers.openBarrier();
    private Integer splits = null;
    private boolean manualShutdown = false;
    private boolean once = false;
    private boolean safe = false;
    private boolean logErrors = false;
    private boolean valid = true;
    
    public ExecConfigBuilder tasks(Collection<Task> tasks) {
        this.tasks = new ArrayList<Task>(tasks);
        return this;
    }
    
    public ExecConfigBuilder runnables(Collection<Runnable> runnables) {
        this.tasks = new ArrayList<Task>(tasks.size());
        
        for (Runnable task : runnables) {
            this.tasks.add(new RunnableAdapter(task, true));
        }
        
        return this;
    }
    
    public ExecConfigBuilder callables(Collection<Callable<?>> callables) {
        this.tasks = new ArrayList<Task>(tasks.size());
        
        for (Callable<?> task : callables) {
            this.tasks.add(new CallableAdapter(task, true));
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
    
    public ExecConfigBuilder manualShutdown() {
        this.manualShutdown = true;
        return this;
    }
    
    public ExecConfigBuilder once() {
        this.once = true;
        return this;
    }
    
    public ExecConfigBuilder safe() {
        this.safe = true;
        return this;
    }

    public ExecConfigBuilder logErrors() {
        this.logErrors = true;
        return this;
    }
    
    public ExecConfigBuilder split(int splits) {
        if (splits < 1) {
            throw new IllegalArgumentException("splits < 1");
        }
        this.splits = splits;
        return this;
    }
    
    private boolean valid() {
        return valid && (tasks != null) && (condition != null) && (barrier != null);
    }
    
    public ExecConfig build() {
        if (!valid()) {
            throw new IllegalStateException("ExecConfigBuilder state is invalid");
        }
        
        InternalExecConfig result = new InternalExecConfig();
        
        result.condition = once ? ExecConditions.once(tasks) : condition;
        result.barrier = barrier;
        result.manualShutdown = manualShutdown;
        
        ListIterator<Task> iter = tasks.listIterator();
        while (iter.hasNext()) {
            Task task = iter.next();
            
            if (logErrors) {
                task = new LoggingTask(task);
            }
            
            if (safe) {
                task = new SafeTask(task);
            }
            
            iter.set(task);
        }
        
        if (splits != null && tasks.size() < splits && tasks.size() > 0) {
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
        protected BlockingBarrier barrier;
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
        public BlockingBarrier getBarrier() {
            return barrier;
        }

        @Override
        public boolean isManualShutdown() {
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
        public void cancel(Interruptible thread) throws Exception {
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
        public void cancel(Interruptible thread) throws Exception {
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
    
    private static class RunnableAdapter extends AbstractTask implements DelegatingTask  {
        private final Runnable delegate;
        
        public RunnableAdapter(Runnable delegate, boolean interrupt) {
            super(interrupt);
            this.delegate = delegate;
        }

        @Override
        public void run() throws Exception {
            delegate.run();
        }
        
        @Override
        public Object getDelegate() {
            return delegate;
        }
    }
    
    private static class CallableAdapter extends AbstractTask implements DelegatingTask {
        private final Callable<?> delegate;
        
        public CallableAdapter(Callable<?> delegate, boolean interrupt) {
            super(interrupt);
            this.delegate = delegate;
        }

        @Override
        public void run() throws Exception {
            delegate.call();
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
        public void cancel(Interruptible thread) throws Exception {
            curTask.cancel(thread);
        }
        
        @Override
        public Object getDelegate() {
            return curTask;
        }
    }
}

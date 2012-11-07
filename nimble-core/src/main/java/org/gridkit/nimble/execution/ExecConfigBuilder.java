package org.gridkit.nimble.execution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;

import org.gridkit.util.concurrent.Barriers;
import org.gridkit.util.concurrent.BlockingBarrier;

// add task errors logging and safe tasks
public class ExecConfigBuilder {
    protected Collection<Task> tasks = Collections.emptySet();
    protected ExecCondition condition = ExecConditions.infinity();
    protected BlockingBarrier barrier = Barriers.openBarrier();
    protected boolean continuous = false;
    protected boolean once = false;
    
    public ExecConfigBuilder tasks(Collection<Task> tasks) {
        this.tasks = tasks;
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
    
    public ExecConfigBuilder continuous(boolean continuous){
        this.continuous = continuous;
        return this;
    }
    
    public ExecConfigBuilder once() {
        this.once = true;
        return this;
    }

    private boolean valid() {
        return (tasks != null) && (condition != null) && (barrier != null);
    }
    
    public ExecConfig build() {
        if (!valid()) {
            throw new IllegalStateException("ExecConfigBuilder state is invalid to create new ExecConfig");
        }
        
        InternalExecConfig result = new InternalExecConfig();
        
        result.tasks = tasks;
        result.condition = once ? ExecConditions.once(tasks) : condition;
        result.barrier = barrier;
        result.continuous = continuous;

        return result;
    }
    
    private static class InternalExecConfig implements ExecConfig {
        protected Collection<Task> tasks;
        protected ExecCondition condition;
        protected BlockingBarrier barrier;
        protected boolean continuous;
        
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
        public boolean isContinuous() {
            return continuous;
        }
    }
    
    private static class RunnableAdapter extends AbstractTask {
        private final Runnable delegate;
        
        public RunnableAdapter(Runnable delegate, boolean interrupt) {
            super(interrupt);
            this.delegate = delegate;
        }

        @Override
        public void run() throws Exception {
            delegate.run();
        }
        
        
    }
    
    private static class CallableAdapter extends AbstractTask {
        private final Callable<?> delegate;
        
        public CallableAdapter(Callable<?> delegate, boolean interrupt) {
            super(interrupt);
            this.delegate = delegate;
        }

        @Override
        public void run() throws Exception {
            delegate.call();
        }
    }
}

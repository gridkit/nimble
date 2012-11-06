package org.gridkit.nimble.execution;

import org.gridkit.util.concurrent.Barriers;
import org.gridkit.util.concurrent.BlockingBarrier;

public class ExecConfigBuilder {
    protected TaskProvider provider;
    protected ExecCondition condition = ExecConditions.infinity();
    protected BlockingBarrier barrier = Barriers.openBarrier();
    protected boolean continuous = true;
    protected int threads = 1;
    
    public ExecConfigBuilder provider(TaskProvider provider) {
        this.provider = provider;
        return this;
    }
    
    public ExecConfigBuilder condition(ExecCondition condition) {
        this.condition = condition;
        return this;
    }
    
    public ExecConfigBuilder barrier(BlockingBarrier barrier){
        this.barrier = barrier;
        return this;
    }
    
    public ExecConfigBuilder threads(int threads){
        this.threads = threads;
        return this;
    }
    
    public ExecConfigBuilder continuous(boolean continuous){
        this.continuous = continuous;
        return this;
    }
    
    private boolean valid() {
        return (provider != null) && (condition != null) && (barrier != null) && (threads > 0);
    }
    
    public ExecConfig build() {
        if (!valid()) {
            throw new IllegalStateException();
        }
        
        InternalExecConfig result = new InternalExecConfig();
        
        result.provider = provider;
        result.condition = condition;
        result.barrier = barrier;
        result.continuous = continuous;
        result.threads = threads;

        return result;
    }
    
    private static class InternalExecConfig implements ExecConfig {
        protected TaskProvider provider;
        protected ExecCondition condition;
        protected BlockingBarrier barrier;
        protected boolean continuous;
        protected int threads;
        
        @Override
        public TaskProvider getProvider() {
            return provider;
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
        public int getThreads() {
            return threads;
        }

        @Override
        public boolean isContinuous() {
            return continuous;
        }
    }
}

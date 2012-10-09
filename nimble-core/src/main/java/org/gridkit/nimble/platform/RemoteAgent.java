package org.gridkit.nimble.platform;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.Callable;

import com.google.common.util.concurrent.ListenableFuture;

public interface RemoteAgent {    
    Set<String> getLabels();
        
    void shutdown(boolean hard);
    
    <T> ListenableFuture<T> invoke(Invocable<T> invocable);
    
    public interface Invocable<T> extends Serializable {
        T invoke(LocalAgent localAgent) throws Exception;
    }
    
    @SuppressWarnings("serial")
    public static class CallableInvocable<T> implements Invocable<T>, Serializable, Callable<T> {
        private final Callable<T> delegate;

        public CallableInvocable(Callable<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public T invoke(LocalAgent localAgent) throws Exception {
            return delegate.call();
        }

        @Override
        public T call() throws Exception {
            return delegate.call();
        }
        
        public String toString() {
        	return delegate.toString();
        }
    }
}

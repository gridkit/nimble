package org.gridkit.nimble.platform.local;

import static org.gridkit.nimble.util.StringOps.F;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

import org.gridkit.nimble.platform.LocalAgent;
import org.gridkit.nimble.platform.RemoteAgent;
import org.gridkit.nimble.platform.SystemTimeService;
import org.gridkit.nimble.platform.TimeService;
import org.gridkit.nimble.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class ThreadPoolAgent implements LocalAgent, RemoteAgent {
    private final Set<String> labels;
        
    private final ListeningExecutorService executor;
    
    private final ConcurrentMap<String, Object> attrs;
    
    public ThreadPoolAgent(Set<String> labels) {
        this.labels = new HashSet<String>(labels);
        
        String threadGroup = F("%s[%d,%s]", ThreadPoolAgent.class.getSimpleName(), System.identityHashCode(this), labels);
        this.executor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(new NamedThreadFactory(threadGroup)));
        
        this.attrs = new ConcurrentHashMap<String, Object>();
    }
    
    public ThreadPoolAgent() {
        this(Collections.<String>emptySet());
    }
    
    @Override
    public Set<String> getLabels() {
        return Collections.unmodifiableSet(labels);
    }

    @Override
    public <T> ListenableFuture<T> invoke(final Invocable<T> invocable) {
        return executor.submit(new Callable<T>() {
            @Override
            public T call() throws Exception {
                return invocable.invoke(ThreadPoolAgent.this);
            }
        });
    }

    @Override
    public Logger getLogger(String name) {
        return LoggerFactory.getLogger(ThreadPoolAgent.class.getSimpleName() + "." + name);
    }

    @Override
    public ConcurrentMap<String, Object> getAttrsMap() {
        return attrs;
    }
    
    @Override
    public TimeService getTimeService() {
        return SystemTimeService.getInstance();
    }
    
    @Override
    public void shutdown(boolean hard) {
        if (hard) {
            executor.shutdownNow();
        } else {
            executor.shutdown();
        }
    }
    
    @Override
    public String toString() {
        return F("ThreadPoolAgent[%s,%d]", labels, System.identityHashCode(this));
    }
}

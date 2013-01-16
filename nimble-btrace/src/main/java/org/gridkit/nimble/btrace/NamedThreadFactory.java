package org.gridkit.nimble.btrace;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

class NamedThreadFactory implements ThreadFactory {

	private final String name;
    private final ThreadGroup group;
    private final boolean daemonStatus;
    private final int priority;
    
    private final AtomicInteger threadNumber;
    
    public NamedThreadFactory(String name, boolean daemonStatus, int priority) {
        this.name = name;
        this.group = new ThreadGroup(name);
        this.daemonStatus = daemonStatus;
        this.priority = priority;
        
        this.threadNumber = new AtomicInteger(0);
    }
    
    public NamedThreadFactory(String name) {
        this(name, false, Thread.NORM_PRIORITY);
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r, name + "-" + threadNumber.getAndIncrement(), 0);
        
        t.setDaemon(daemonStatus);
        t.setPriority(priority);
        
        return t;
    }
}

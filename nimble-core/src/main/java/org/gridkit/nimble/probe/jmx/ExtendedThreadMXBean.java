package org.gridkit.nimble.probe.jmx;

import java.lang.management.ThreadMXBean;

/**
 * Copy of com.sun.management.ThreadMXBean
 */
public interface ExtendedThreadMXBean extends ThreadMXBean {
    long[] getThreadCpuTime(long[] ids);
    
    long[] getThreadUserTime(long[] ids);
    
    long getThreadAllocatedBytes(long ids);
    
    long[] getThreadAllocatedBytes(long[] ids);
    
    boolean isThreadAllocatedMemorySupported();
    
    boolean isThreadAllocatedMemoryEnabled();
    
    void setThreadAllocatedMemoryEnabled(boolean enable);
}

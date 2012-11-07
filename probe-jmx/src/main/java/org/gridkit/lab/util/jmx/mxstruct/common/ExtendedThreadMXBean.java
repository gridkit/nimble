package org.gridkit.lab.util.jmx.mxstruct.common;

import java.lang.management.ThreadMXBean;

/**
 * com.sun.management.ThreadMXBean with additional bulk operations
 * available in HotSpot JVM.
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

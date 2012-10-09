package org.gridkit.nimble.platform;

public class SystemTimeService implements TimeService {
    private static TimeService Instance = new SystemTimeService();
    
    public static TimeService getInstance() {
        return Instance;
    }

    protected SystemTimeService() {
        
    }
    
    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public long currentTimeNanos() {
        return System.nanoTime();
    }
}

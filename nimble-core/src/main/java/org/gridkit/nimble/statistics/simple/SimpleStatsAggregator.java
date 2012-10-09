package org.gridkit.nimble.statistics.simple;

import java.rmi.Remote;

public interface SimpleStatsAggregator extends Remote {
    void add(SimpleStats stats);
    
    SimpleStats calculate();
}

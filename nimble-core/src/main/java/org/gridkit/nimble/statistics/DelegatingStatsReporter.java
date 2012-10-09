package org.gridkit.nimble.statistics;

import java.io.Serializable;
import java.util.Map;

@SuppressWarnings("serial")
public class DelegatingStatsReporter implements StatsReporter, Serializable {
    private final StatsReporter delegate;

    public DelegatingStatsReporter(StatsReporter delegate) {
        this.delegate = delegate;
    }

    @Override
    public void report(Map<String, Object> stats) {
        delegate.report(stats);
    }
    
    protected StatsReporter getDelegate() {
        return delegate;
    }
}

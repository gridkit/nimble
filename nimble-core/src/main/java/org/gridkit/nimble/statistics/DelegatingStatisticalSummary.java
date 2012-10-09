package org.gridkit.nimble.statistics;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;

public class DelegatingStatisticalSummary implements StatisticalSummary {
    private final StatisticalSummary delegate;

    public DelegatingStatisticalSummary(StatisticalSummary delegate) {
        this.delegate = delegate;
    }

    @Override
    public double getMean() {
        return delegate.getMean();
    }

    @Override
    public double getVariance() {
        return delegate.getVariance();
    }

    @Override
    public double getStandardDeviation() {
        return delegate.getStandardDeviation();
    }

    @Override
    public double getMax() {
        return delegate.getMax();
    }

    @Override
    public double getMin() {
        return delegate.getMin();
    }

    @Override
    public long getN() {
        return delegate.getN();
    }

    @Override
    public double getSum() {
        return delegate.getSum();
    }
    
    protected StatisticalSummary getDelegate() {
        return delegate;
    }
}

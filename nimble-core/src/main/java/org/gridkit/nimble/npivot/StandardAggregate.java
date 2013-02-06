package org.gridkit.nimble.npivot;

import java.io.Serializable;

public class StandardAggregate implements Aggregate, Serializable {
    private static final long serialVersionUID = 1645423419573050501L;
    
    private final Query query;
    private final SampleSet samples;
    private final QueryProcessor queryProcessor;

    public StandardAggregate(Query query, SampleSet samples, QueryProcessor queryProcessor) {
        this.query = query;
        this.samples = samples;
        this.queryProcessor = queryProcessor;
    }

    @Override
    public Query query() {
        return query;
    }

    @Override
    public SampleSet samples() {
        return samples;
    }

    @Override
    public Aggregate aggregate(Query query) {
        return queryProcessor.aggregate(this, query);
    }
}

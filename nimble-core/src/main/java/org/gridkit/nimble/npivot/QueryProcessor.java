package org.gridkit.nimble.npivot;

public interface QueryProcessor {
    Aggregate aggregate(Aggregate aggr, Query query);
}

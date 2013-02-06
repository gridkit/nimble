package org.gridkit.nimble.npivot;

public interface Cube {
    /**
     * Filter by measure value is not allowed
     * Group by measure value is not allowed
     */
    Aggregate query(Query query);
}

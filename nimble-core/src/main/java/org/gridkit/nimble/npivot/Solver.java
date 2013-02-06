package org.gridkit.nimble.npivot;

import java.util.Set;

public interface Solver {
    Projector project(Set<Object> preimage, Set<Object> image);
    
    Filter adapt(Filter original, Filter required, Set<Object> groups);
}

package org.gridkit.nimble.npivot;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class QueryBuilder {        
    private Filter filter = Filters.constant(true);
    private Set<Object> groups = Collections.emptySet();
    private Set<Object> measures = Collections.emptySet();;

    public QueryBuilder groups(Collection<? extends Object> groups) {
        this.groups = new HashSet<Object>();
        this.groups.addAll(groups);
        return this;
    }
    
    public QueryBuilder measures(Collection<? extends Object> measures) {
        this.measures = new HashSet<Object>();
        this.measures.addAll(measures);
        return this;
    }
    
    public QueryBuilder groups(Object... groups) {
        return groups(Arrays.asList(groups));
    }
    
    public QueryBuilder measures(Object... measures) {
        return measures(Arrays.asList(measures));
    }
    
    public QueryBuilder filter(Filter filter) {
        this.filter = filter;
        return this;
    }
        
    public Query build() {
        Set<Object> groups = new HashSet<Object>(this.groups);
        Set<Object> measures = new HashSet<Object>(this.measures);
        return new StandardQuery(filter, groups, measures);
    }
}

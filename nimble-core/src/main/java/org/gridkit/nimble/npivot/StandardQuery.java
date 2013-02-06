package org.gridkit.nimble.npivot;

import java.io.Serializable;
import java.util.Set;

public class StandardQuery implements Query, Serializable {
    private static final long serialVersionUID = -7728258748245319402L;
    
    private final Filter filter;
    private final Set<Object> groups;
    private final Set<Object> measures;

    public StandardQuery(Filter filter, Set<Object> groups, Set<Object> measures) {
        this.filter = filter;
        this.groups = groups;
        this.measures = measures;
    }

    @Override
    public Filter filter() {
        return filter;
    }

    @Override
    public Set<Object> groups() {
        return groups;
    }

    @Override
    public Set<Object> measures() {
        return measures;
    }

    @Override
    public String toString() {
        return "StandardQuery [filter=" + filter + ", groups=" + groups
                + ", measures=" + measures + "]";
    }
}

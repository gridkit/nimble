package org.gridkit.nimble.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SetOps {
    public static <T> Set<T> intersection(Collection<T> c1, Collection<T> c2) {
        Set<T> result = new HashSet<T>(c1);
        result.retainAll(c2);
        return result;
    }
}

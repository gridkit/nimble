package org.gridkit.nimble.npivot;

import java.util.Set;

public class Projectors {
    public static Projector groupBy(Set<Object> groups) {
        return new GroupByProjector(groups);
    }
    
    public static class GroupByProjector implements Projector {
        private final Set<Object> groups;

        public GroupByProjector(Set<Object> groups) {
            this.groups = groups;
        }

        @Override
        public void project(Sample sample, SettableSample projection) {
            for (Object group : groups) {
                if (sample.keys().contains(group)) {
                    projection.set(group, sample.get(sample));
                } else if (group instanceof Function) {
                    Function func = (Function)group;
                    projection.set(group, func.apply(sample));
                }
            }
        }
    }
}

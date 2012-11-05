package org.gridkit.nimble.execution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class TaskProviders {
    public static TaskProvider loop(Task... tasks) {
        return new ListTaskProvider(Arrays.asList(tasks), true);
    }

    public static TaskProvider loop(Collection<Task> tasks) {
        return new ListTaskProvider(new ArrayList<Task>(tasks), true);
    }
    
    public static TaskProvider list(Task... tasks) {
        return new ListTaskProvider(Arrays.asList(tasks), false);
    }
    
    public static TaskProvider list(Collection<Task> tasks) {
        return new ListTaskProvider(new ArrayList<Task>(tasks), false);
    }

    private static class ListTaskProvider implements TaskProvider {
        private final List<Task> tasks;
        private final boolean cyclic;
        private final AtomicLong index;
        
        public ListTaskProvider(List<Task> tasks, boolean cyclic) {
            this.tasks = tasks;
            this.cyclic = cyclic;
            this.index = new AtomicLong(0);
        }
        
        @Override
        public Task nextTask() {
            long nextIndex = index.getAndIncrement();
            
            if (tasks.isEmpty()) {
                return null;
            } else if (!cyclic && nextIndex >= tasks.size()) {
                return null;
            } else {
                return tasks.get((int)(nextIndex % tasks.size()));
            }
        }
    }
}

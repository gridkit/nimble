package org.gridkit.nimble.btrace;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class CriticalSection {
    private ConcurrentMap<Object, Pair<Integer, Object>> guards = new ConcurrentHashMap<Object, Pair<Integer, Object>>();
    
    public <T> T execute(Object lock, Callable<T> executor) throws Exception {
        Object guard = null;
        
        try {
            guard = enter(lock);
            synchronized (guard) {
                return executor.call();
            }
        } finally {
            if (guard != null) {
                leave(lock);
            }
        }
    }
    
    private Object enter(Object lock) {
        while(true) {
            Pair<Integer, Object> oldGuard = getGuard(lock);
            Pair<Integer, Object> newGuard = Pair.newPair(oldGuard.getA() + 1, oldGuard.getB());
            
            if (guards.replace(lock, oldGuard, newGuard)) {
                return newGuard.getB();
            }
        }
    }
    
    private void leave(Object lock) {
        while (true) {
            Pair<Integer, Object> oldGuard = getGuard(lock);
            
            if (oldGuard.getA() == 1) { // last executor in critical section
                if (guards.remove(lock, oldGuard)) {
                    return;
                }
            } else {
                Pair<Integer, Object> newGuard = Pair.newPair(oldGuard.getA() - 1, oldGuard.getB());
                if (guards.replace(lock, oldGuard, newGuard)) {
                    return;
                }
            }
        }
    }
    
    private Pair<Integer, Object> getGuard(Object lock) {
        Pair<Integer, Object> result = guards.get(lock);
        
        if (result == null) {
            Pair<Integer, Object> value = Pair.newPair(0, new Object());
            
            result = guards.putIfAbsent(lock, value);
            
            if (result == null) {
                result = value;
            }
        }

        return result;
    }
}

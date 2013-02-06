package org.gridkit.nimble.npivot;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Samples {
    public static class MapSample implements SettableSample, Serializable {
        private static final long serialVersionUID = -5424873535309401576L;
        
        private final Map<Object, Object> attrs;

        public MapSample() {
            this(new HashMap<Object, Object>());
        }
        
        public MapSample(Sample sample) {
            this.attrs = new HashMap<Object, Object>();
            
            for (Object key : sample.keys()) {
                attrs.put(key, sample.get(key));
            }
        }
        
        public MapSample(Map<Object, Object> attrs) {
            this.attrs = attrs;
        }

        @Override
        public Set<Object> keys() {
            return Collections.unmodifiableSet(attrs.keySet());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key) {
            return (T)attrs.get(key);
        }

        @Override
        public void set(Object key, Object value) {
            attrs.put(key, value);
        }

        @Override
        public String toString() {
            return "MapSample [" + attrs + "]";
        }
    }
    
    public static class SubsetSample implements Sample, Serializable {
        private static final long serialVersionUID = 3979891386784391817L;
        
        private final Sample delegate;
        private final Set<Object> keys;

        public SubsetSample(Sample delegate, Set<Object> keys) {
            this.delegate = delegate;
            this.keys = keys;
        }

        @Override
        public Set<Object> keys() {
            HashSet<Object> result = new HashSet<Object>();
            
            result.addAll(delegate.keys());
            result.retainAll(keys);
            
            return result;
        }

        @Override
        public <T> T get(Object key) {
            if (keys.contains(key)) {
                return delegate.get(key);
            } else {
                return null;
            }
        }
    }
}

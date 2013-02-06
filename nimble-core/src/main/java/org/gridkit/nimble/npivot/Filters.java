package org.gridkit.nimble.npivot;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

public class Filters {
    public static Filter constant(boolean value) {
        return new ConstantFilter(value);
    }
    
    public static class ConstantFilter implements Filter, Serializable {
        private static final long serialVersionUID = 6854006036499047334L;
        
        private final boolean value;

        public ConstantFilter(boolean value) {
            this.value = value;
        }

        @Override
        public Set<Object> parameters() {
            return Collections.emptySet();
        }

        @Override
        public Boolean apply(Sample sample) {
            return value;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (value ? 1231 : 1237);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ConstantFilter other = (ConstantFilter) obj;
            if (value != other.value)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "ConstantFilter [" + value + "]";
        }
    }
}

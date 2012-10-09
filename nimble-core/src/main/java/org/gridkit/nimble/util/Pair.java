package org.gridkit.nimble.util;

import java.io.Serializable;
import java.util.Map;

@SuppressWarnings("serial")
public class Pair<A, B> implements Serializable, Map.Entry<A, B> {
    private final A a;
    private final B b;
    
    public Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }
    
    public static <A,B> Pair<A,B> newPair(A a, B b) {
        return new Pair<A,B>(a,b);
    }

    public A getA() {
        return a;
    }

    public B getB() {
        return b;
    }

    @Override
    public A getKey() {
        return a;
    }

    @Override
    public B getValue() {
        return b;
    }

    @Override
    public B setValue(B value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "Pair [" + a + ", " + b + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((a == null) ? 0 : a.hashCode());
        result = prime * result + ((b == null) ? 0 : b.hashCode());
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
        Pair<?, ?> other = (Pair<?, ?>) obj;
        if (a == null) {
            if (other.a != null)
                return false;
        } else if (!a.equals(other.a))
            return false;
        if (b == null) {
            if (other.b != null)
                return false;
        } else if (!b.equals(other.b))
            return false;
        return true;
    }
}

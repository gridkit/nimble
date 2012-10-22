package org.gridkit.nimble.pivot;

import java.io.Serializable;
import java.util.List;

public class Decorated implements Serializable {

	private final List<Object> decorator;
	private final Object key;
	
	public Decorated(List<Object> decorator, Object key) {
		this.decorator = decorator;
		this.key = key;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((decorator == null) ? 0 : decorator.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
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
		Decorated other = (Decorated) obj;
		if (decorator == null) {
			if (other.decorator != null)
				return false;
		} else if (!decorator.equals(other.decorator))
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return decorator.toString() + key;
	}	
}

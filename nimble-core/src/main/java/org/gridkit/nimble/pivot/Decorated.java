package org.gridkit.nimble.pivot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Decorated implements Serializable {

	private static final long serialVersionUID = 20121022L;
	
	private final List<Object> decoration;
	private final Object key;
	
	public Decorated(List<Object> decoration, Object key) {
		this.decoration = new ArrayList<Object>(decoration);
		this.key = key;
	}

	public Decorated(List<Object> decoration, Object lastDeco, Object key) {
		this.decoration = new ArrayList<Object>(decoration.size() + 1);
		this.decoration.addAll(decoration);
		this.decoration.add(lastDeco);
		this.key = key;
	}
	
	public List<Object> getDecoration() {
		return decoration;
	}
	 
	public boolean startsWith(List<Object> deco) {
		if (deco.size() <= decoration.size()) {
			for(int i = 0; i != deco.size(); ++i) {
				if (!deco.get(i).equals(decoration.get(i))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	public Object getOriginalKey() {
		return key;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((decoration == null) ? 0 : decoration.hashCode());
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
		if (decoration == null) {
			if (other.decoration != null)
				return false;
		} else if (!decoration.equals(other.decoration))
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
		return decoration.toString() + key;
	}	
}

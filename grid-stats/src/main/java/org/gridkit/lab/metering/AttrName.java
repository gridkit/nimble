package org.gridkit.lab.metering;

import java.io.Serializable;

public class AttrName implements Serializable {

	public static AttrName attr(String name) {
		// TODO
		throw new UnsupportedOperationException();
	}
	
	private final String className;
	private final String attribute;
	
	AttrName(String className, String attribute) {
		this.className = className;
		this.attribute = attribute;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((attribute == null) ? 0 : attribute.hashCode());
		result = prime * result
				+ ((className == null) ? 0 : className.hashCode());
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
		AttrName other = (AttrName) obj;
		if (attribute == null) {
			if (other.attribute != null)
				return false;
		} else if (!attribute.equals(other.attribute))
			return false;
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return clip(className) + "." + attribute;
	}
	
	private Object clip(String line) {
		return line.indexOf('.') > 0 ? line.substring(line.lastIndexOf('.') + 1, line.length()) : line;
	}	
}

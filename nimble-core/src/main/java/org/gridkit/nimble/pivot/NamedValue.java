package org.gridkit.nimble.pivot;

public class NamedValue {

	private final String name;
	private final Object value;
	
	public NamedValue(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public Object getValue() {
		return value;
	}
}

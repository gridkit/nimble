package org.gridkit.nimble.pivot.display;

public interface UnitDeco {

	public double getMultiplier();
	
	public UnitDecoType getType();
	
	enum UnitDecoType {
		NUMERATOR,
		DENOMINATOR
	}
	
}

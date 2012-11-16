package org.gridkit.nimble.pivot.display;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import org.gridkit.nimble.pivot.display.UnitDeco.UnitDecoType;

public class Units {

	public static UnitDeco MILLIS = new SimpleUnitDeco("Millis", TimeUnit.SECONDS.toMillis(1), UnitDecoType.NUMERATOR);
	public static UnitDeco KiB = new SimpleUnitDeco("KiB", 1d / (1 <<10), UnitDecoType.NUMERATOR);
	public static UnitDeco MiB = new SimpleUnitDeco("MiB", 1d / (1 <<20), UnitDecoType.NUMERATOR);
	public static UnitDeco GiB = new SimpleUnitDeco("GiB", 1d / (1 <<30), UnitDecoType.NUMERATOR);
	public static UnitDeco K = new SimpleUnitDeco("K", 1e-3d, UnitDecoType.NUMERATOR);
	public static UnitDeco M = new SimpleUnitDeco("M", 1e-6d, UnitDecoType.NUMERATOR);

	public static UnitDeco PERCENT = new SimpleUnitDeco("%", 100, UnitDecoType.NUMERATOR);
	
	
	private static class SimpleUnitDeco implements UnitDeco, Serializable {
		
		private static final long serialVersionUID = 20121024L;
		
		final String name;
		final double multiplier;
		final UnitDecoType type;
		
		public SimpleUnitDeco(String name, double multiplier, UnitDecoType type) {
			this.name = name;
			this.multiplier = multiplier;
			this.type = type;
		}

		@Override
		public double getMultiplier() {
			return multiplier;
		}
		
		@Override
		public UnitDecoType getType() {
			return type;
		}
		
		public String toString() {
			return name;
		}
	}	
}

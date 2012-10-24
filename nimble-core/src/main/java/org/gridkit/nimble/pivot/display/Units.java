package org.gridkit.nimble.pivot.display;

import java.util.concurrent.TimeUnit;

public class Units {

	private static double MILLIS_PER_SEC = TimeUnit.SECONDS.toMillis(1);
	
	public static UnitDeco MILLIS = new UnitDeco() {
		
		@Override
		public double transalte(double source) {
			return MILLIS_PER_SEC * source;
		}
		
		public String toString() {
			return "Millis";
		}
	};

	public static UnitDeco KiB = new UnitDeco() {
		
		@Override
		public double transalte(double source) {
			return source / (1 << 10);
		}
		
		public String toString() {
			return "KiB";
		}		
	};

	public static UnitDeco MiB = new UnitDeco() {
		
		@Override
		public double transalte(double source) {
			return source / (1 << 20);
		}
		
		public String toString() {
			return "MiB";
		}				
	};

	public static UnitDeco GiB = new UnitDeco() {
		
		@Override
		public double transalte(double source) {
			return source / (1 << 30);
		}
		
		public String toString() {
			return "GiB";
		}				
	};	
}

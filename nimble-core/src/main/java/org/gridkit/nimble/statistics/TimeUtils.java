package org.gridkit.nimble.statistics;

import java.util.concurrent.TimeUnit;

public class TimeUtils {
	
	private static double S = TimeUnit.SECONDS.toNanos(1);
	private static double N = TimeUnit.MILLISECONDS.toNanos(1);
	
	private static long NANO_ANCHOR = System.nanoTime();
	private static double MILLIS_ANCHOR = N * System.currentTimeMillis();
	
	public static double normalize(long nanotime) {
		return (MILLIS_ANCHOR + (nanotime - NANO_ANCHOR)) / S;
	}
	
	public static long toMillis(double epoc) {
		return (long)(1000 * epoc);
	}

	public static double toSeconds(double nanos) {
		return nanos / S;
	}

}

package org.gridkit.nimble.metering;

public interface SampleWriter {

	/** Short cut for {code}set(Measure.MEASURE, measure){code} */
	public SampleWriter setMeasure(double measure);

	/** 
	 * Short cut for {code}set(Measure.TIMESTAMP, timestamp){code}
	 * 
	 * @param timestampNs expected raw {@link System#nanoTime()}
	 */
	public SampleWriter setTimestamp(long timestampNs);
	
	public SampleWriter setTimeAndDuration(double startS, double durationS);

	/**
	 * @param startNs expected raw {@link System#nanoTime()}
	 * @param finishNs expected raw {@link System#nanoTime()}
	 */
	public SampleWriter setTimeBounds(long startNs, long finishNs);
	
	public SampleWriter set(Object key, int value);

	public SampleWriter set(Object key, long value);

	public SampleWriter set(Object key, double value);

	public SampleWriter set(Object key, Object value);

	public SampleWriter set(Object key, String value);

	public void submit();
	
}

package org.gridkit.nimble.metering;

public interface SampleWriter {

	/** Short cut for {code}set(Measure.MEASURE, measure){code} */
	public SampleWriter setMeasure(double measure);

	/** Short cut for {code}set(Measure.TIMESTAMP, timestamp){code} */
	public SampleWriter setTimestamp(long timestampNs);
	
	public SampleWriter setTimeAndDuration(long startNs, long durationNs);

	public SampleWriter setTimeBounds(long startNs, long finishNs);
	
	public SampleWriter set(Object key, int value);

	public SampleWriter set(Object key, long value);

	public SampleWriter set(Object key, double value);

	public SampleWriter set(Object key, Object value);

	public SampleWriter set(Object key, String value);

	public void submit();
	
}

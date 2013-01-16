package org.gridkit.nimble.btrace;

public interface BTraceScriptSampler {

	public void reportScalar(String store, String metric, double value);
	
	public void reportPoint(String store, String metric, double timestampS, double value);
	
	public void reportDuration(String store, String metric, double timestampS, long durationNS);

	public void reportSpan(String store, String metric, double timestampS, long durationNS, double value);
	
	public void reportMissedSamples(String store, double timestamp, long count);
	
}

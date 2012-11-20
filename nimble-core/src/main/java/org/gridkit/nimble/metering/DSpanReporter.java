package org.gridkit.nimble.metering;

/**
 * Descriminating version of {@link TimeReporter}.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface DSpanReporter<T extends Enum<T>> {
	
	public StopWatch<T> start();
	
	public interface StopWatch<T extends Enum<T>> {

		public void stop(double measure, T descriminator);
		
	}
}

package org.gridkit.nimble.statistics;

/**
 * This a marker interface for various statistical summaries of metrics.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface Summary {

	public interface CountSummary extends Summary {

		public long getN();
		
	}

	public interface SumSummary extends Summary {
		
		public double getSum();
		
	}
	
}

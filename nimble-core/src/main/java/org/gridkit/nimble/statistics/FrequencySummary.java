package org.gridkit.nimble.statistics;

import java.io.Serializable;

public interface FrequencySummary extends Summary, Summary.CountSummary, Summary.SumSummary {

	/**
	 * @return as epoc seconds
	 */
	public double getEarliestEventTimestamp();
	
	/**
	 * @return as epoc seconds
	 */
	public double getLatestEventTimestamp();
	
	/**
	 * @return frequency unit / sec
	 */
	public double getWeigthedFrequency();
	
	/**
	 * @return frequency event / sec
	 */
	public double getEventFrequency();
	
	@Override
	public double getSum();
	
	/**
	 * 
	 * @return measures interval duration is seconds
	 */
	public double getDuration();
	
	public static class Values implements FrequencySummary, Serializable {
		
		private static final long serialVersionUID = 20121017L;
		
		protected long n;
		protected double total;
		protected double first;
		protected double last;
		
		public Values(long n, double total, double first, double last) {
			this.n = n;
			this.total = total;
			this.first = first;
			this.last = last;
		}

		@Override
		public long getN() {
			return n;
		}

		@Override
		public double getEarliestEventTimestamp() {
			return first;
		}

		@Override
		public double getLatestEventTimestamp() {
			return last;
		}

		@Override
		public double getWeigthedFrequency() {
			return total / getDuration();
		}

		@Override
		public double getEventFrequency() {
			return n / getDuration();
		}

		@Override
		public double getSum() {
			return total;
		}

		@Override
		public double getDuration() {
			return last - first;
		}
		
		public String toString() {
			return "Summary{n: " + getN() + ", freq: " + getWeigthedFrequency() + ", duration: " + getDuration() + "}";
		}
	}
}

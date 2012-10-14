package org.gridkit.nimble.statistics;

public interface FrequencySummary extends SampleSummary {

	/**
	 * @return as normalized nanotime
	 */
	public double getEarliestEventTimestamp();
	
	/**
	 * @return as normalized nanotime
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
	
	public double getTotal();
	
	/**
	 * 
	 * @return measures interval duration is seconds
	 */
	public double getDuration();
	
	public static class Values implements FrequencySummary {
		
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
		public double getTotal() {
			return total;
		}

		@Override
		public double getDuration() {
			return last - first;
		}
	}
}

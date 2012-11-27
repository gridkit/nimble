package org.gridkit.nimble.statistics;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public interface DistinctSummary extends Summary, Summary.CountSummary {

	public long getTotalCount();
	
	public int getDistictCount();
	
	public long getValueCount(Object value);
	
	public Set<Object> getDistinctValues();
	
	public static class Values implements DistinctSummary, Serializable {

		private static final long serialVersionUID = 20121108L;
		
		private final long total;
		private final Map<Object, Long> counters;
		
		public Values(Map<Object, Long> counters) {
			this.counters = new HashMap<Object, Long>(counters);
			this.total = total();
		}
		
		@Override
		public boolean isEmpty() {
			return total == 0;
		}

		private long total() {
			long t = 0;
			for(Long n: counters.values()) {
				t += n;
			}
			return t;
		}

		@Override
		public long getTotalCount() {
			return total;
		}

		@Override
		public long getN() {
			return total;
		}

		@Override
		public int getDistictCount() {
			return counters.size();
		}

		@Override
		public long getValueCount(Object value) {
			Long n = counters.get(value);
			return n == null ? 0 : n;
		}

		@Override
		public Set<Object> getDistinctValues() {
			return Collections.unmodifiableSet(counters.keySet());
		}
		
		@Override
		public String toString() {
			return "Summary{n: " + total + ", distinct: " + counters.size() + ", values=" + getDistinctValues() + "}";
		}
	}	
}

package org.gridkit.nimble.metering;

import java.io.Serializable;

public enum Measure {
		
	NAME,
	MEASURE,
	/**
	 * Timestamp for single even or start of interval for time spanning events
	 */
	TIMESTAMP,
	/**
	 * Duration of event for time spanning even
	 */
	DURATION,
	
	;
	
	public static final Summary MEASURE_SUMMARY = new Summary(MEASURE);
	public static final Summary DURATION_SUMMARY = new Summary(DURATION);
	
	public static Summary summary(Object key) {
		return new Summary(key);
	}
	
	public static class Summary implements Serializable {
		
		private static final long serialVersionUID = 20121023L;
		
		private final Object key;
		
		public Summary(	Object key) {
			this.key = key;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Summary other = (Summary) obj;
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			return true;
		}


		@Override
		public String toString() {
			return "Summary[" + key + "]";
		}
	}	
}

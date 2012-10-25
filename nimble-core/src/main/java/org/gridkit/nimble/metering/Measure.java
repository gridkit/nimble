package org.gridkit.nimble.metering;

import java.io.Serializable;

public enum Measure {
		
	NAME,
	MEASURE,
	/**
	 * Timestamp for single even or interval end for time intervals
	 */
	TIMESTAMP,
	END_TIMESTAMP,
	
	;
	
	public static Freq freq(Object key) {
		return new Freq(key);
	}

	public static Distinct distinct(Object key) {
		return new Distinct(key);
	}

	public static Distrib distrib(Object key) {
		return new Distrib(key);
	}
	
	public static class Freq implements Serializable {
		
		private static final long serialVersionUID = 20121023L;
		
		private final Object key;

		public Freq(Object key) {
			super();
			this.key = key;
		}

		public Object getKey() {
			return key;
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
			Freq other = (Freq) obj;
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Freq[" + key + "]" ;
		}
	}	

	public static class Distinct implements Serializable {
		
		private static final long serialVersionUID = 20121023L;
		
		private final Object key;
		
		public Distinct(Object key) {
			super();
			this.key = key;
		}
		
		public Object getKey() {
			return key;
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
			Distinct other = (Distinct) obj;
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Distinct[" + key + "]" ;
		}
	}	

	public static class Distrib implements Serializable {
		
		private static final long serialVersionUID = 20121023L;
		
		private final Object key;
		
		public Distrib(Object key) {
			super();
			this.key = key;
		}
		
		public Object getKey() {
			return key;
		}
		
		@Override
		public String toString() {
			return "Distrib[" + key + "]" ;
		}
	}	
}

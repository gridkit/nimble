package org.gridkit.nimble.pivot;

import java.io.Serializable;

import org.gridkit.nimble.statistics.DistributionSummary;
import org.gridkit.nimble.statistics.FrequencySummary;
import org.gridkit.nimble.statistics.Summary;

/**
 * This is synthetic key to associate aggregations with base variable.
 * @author ragoale
 *
 */
class AggregationKey implements Serializable {

	private static final long serialVersionUID = 20121031L;
	
	public static AggregationKey distribution(Object key) {
		return new AggregationKey(key, DistributionSummary.class);
	}

	public static AggregationKey frequency(Object key) {
		return new AggregationKey(key, FrequencySummary.class);
	}
	
	private final Object summaryKey;
	private final Class<? extends Summary> summaryType;
	
	public AggregationKey(Object summaryKey, Class<? extends Summary> summaryType) {
		this.summaryKey = summaryKey;
		this.summaryType = summaryType;
	}

	public Object getMeasureKey() {
		return summaryKey;
	}
	
	public Class<? extends Summary> getSummaryType() {
		return summaryType;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((summaryKey == null) ? 0 : summaryKey.hashCode());
		result = prime * result
				+ ((summaryType == null) ? 0 : summaryType.hashCode());
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
		AggregationKey other = (AggregationKey) obj;
		if (summaryKey == null) {
			if (other.summaryKey != null)
				return false;
		} else if (!summaryKey.equals(other.summaryKey))
			return false;
		if (summaryType == null) {
			if (other.summaryType != null)
				return false;
		} else if (!summaryType.equals(other.summaryType))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return summaryType.getSimpleName() + "[" + summaryKey + "]";
	}	
}

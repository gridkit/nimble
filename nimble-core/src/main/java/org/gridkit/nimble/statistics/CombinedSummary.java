package org.gridkit.nimble.statistics;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This class bundle one or more statistical aggregation describing variable 
 * and allows type based access to required summary type.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class CombinedSummary implements Serializable {

	private static final long serialVersionUID = 20121031L;
	
	private final Map<Class<?>, Summary> summaryAspects = new HashMap<Class<?>, Summary>();
	
	public <T extends Summary> void addAggregation(Class<T> type, Summary summary) {
		summaryAspects.put(type, summary);
		for(Class<?> i: type.getInterfaces()) {
			if (Summary.class.isAssignableFrom(i)) {
				summaryAspects.put(i, summary);
			}
		}
	}

	public <T extends Summary> T getSummary(Class<T> type) {
		Summary ag = summaryAspects.get(type);
		if (ag == null) {
			return null;
		}
		return type.cast(ag);
	}	
	
	@Override
	public String toString() {
		return "{combined-summary}";
	}
}

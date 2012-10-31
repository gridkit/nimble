package org.gridkit.nimble.statistics;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.gridkit.nimble.pivot.Aggregation;

/**
 * This class bundle one or more statistical aggregation describing variable 
 * and allows type based access to required summary type.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class SummaryAggregation implements Serializable {

	private static final long serialVersionUID = 20121031L;
	
	private final Map<Class<?>, Aggregation<?>> aggregations = new HashMap<Class<?>, Aggregation<?>>();
	private final Map<Class<?>, Aggregation<?>> summaryAspects = new HashMap<Class<?>, Aggregation<?>>();
	
	public <T extends Summary> void addAggregation(Class<T> type, Aggregation<? extends T> aggregation) {
		if (aggregations.containsKey(type)) {
			throw new IllegalArgumentException("Summary: " + type.getSimpleName() + " is already added");
		}
		else {
			aggregations.put(type, aggregation);
			summaryAspects.put(type, aggregation);
			for(Class<?> i: type.getInterfaces()) {
				if (Summary.class.isAssignableFrom(i)) {
					summaryAspects.put(i, aggregation);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public <V, T extends Aggregation<V>> T getAggregation(Class<T> type) {
		return (T) aggregations.get(type);
	}	
	
	public <T extends Summary> T getSummary(Class<T> type) {
		Aggregation<?> ag = summaryAspects.get(type);
		if (ag == null) {
			return null;
		}
		return type.cast(ag.getResult());
	}
	
}

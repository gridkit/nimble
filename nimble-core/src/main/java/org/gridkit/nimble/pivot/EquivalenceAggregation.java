package org.gridkit.nimble.pivot;

import java.io.Serializable;

import org.gridkit.nimble.metering.SampleReader;

class EquivalenceAggregation implements Aggregation<Object> {

	private static Object NO_VALUE = new NoValue();
	
	private Pivot.Extractor extractor;
	private Object value;
	private boolean notSet; 
	
	public EquivalenceAggregation(Pivot.Extractor extractor) {
		this.extractor = extractor;
		this.value = null;
		this.notSet = true;
	}

	@Override
	public void addSamples(SampleReader reader) {
		while(true) {
			Object val = extractor.extract(reader);
			aggregate(val);
			if (!reader.next()) {
				break;
			}
		}
	}

	private void aggregate(Object val) {
		if (value == NO_VALUE || val instanceof NoValue) {
			return;
		}
		if (notSet) {
			value = val;
			notSet = false;
		}
		else if (!equals(value, val)) {
			value = NO_VALUE;
		}				
	}

	private boolean equals(Object v1, Object v2) {
		return v1 == null ? v2 == null : v1.equals(v2);
	}

	@Override
	public void addAggregate(Object aggregate) {
		aggregate(aggregate);
	}

	@Override
	public Object getResult() {
		return value == NO_VALUE ? NO_VALUE : value;
	}
	
	private static class NoValue implements Serializable {

		private static final long serialVersionUID = 20121010L;

		@Override
		public String toString() {
			return "N/A";
		}
	}
}

package org.gridkit.nimble.metering;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


public class MeteringTemplate implements SchemaTemplate, Serializable {

	private static final long serialVersionUID = 20121017L;
	
	private final Map<Object, Object> statics = new HashMap<Object, Object>();
	private Object measureKey = Measure.MEASURE;
	private Object timestampKey = Measure.TIMESTAMP;
	private Object endTimestampKey = Measure.END_TIMESTAMP;
	
	@Override
	public SampleFactory createFactory(SampleSchema rootSchema) {
		SampleSchema ss = rootSchema.createDerivedScheme();
		for(Object key: statics.keySet()) {
			ss.setStatic(key, statics.get(key));
		}
		if (measureKey != null) {
			ss.declareDynamic(measureKey, double.class);
		}
		if (timestampKey != null) {
			ss.declareDynamic(timestampKey, double.class);
		}
		if (endTimestampKey != null) {
			ss.declareDynamic(endTimestampKey, double.class);
		}
		
		return ss.createFactory();
	}

	public Object getMeasureKey() {
		return measureKey;
	}

	public void setMeasureKey(Object measureKey) {
		this.measureKey = measureKey;
	}

	public Object getTimestampKey() {
		return timestampKey;
	}

	public void setTimestampKey(Object timestampKey) {
		this.timestampKey = timestampKey;
	}

	public Object getEndTimestampKey() {
		return endTimestampKey;
	}

	public void setEndTimestampKey(Object endTimestampKey) {
		this.endTimestampKey = endTimestampKey;
	}
}

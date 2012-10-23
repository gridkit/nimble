package org.gridkit.nimble.metering;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.gridkit.nimble.driver.MeteringAware;
import org.gridkit.nimble.driver.MeteringDriver;


public abstract class AbstractSamplerTemplate<T> implements MeteringAware<T>, Serializable {

	private static final long serialVersionUID = 20121017L;
	
	private final Map<Object, Object> statics = new HashMap<Object, Object>();
	protected Object measureKey = Measure.MEASURE;
	protected Object timestampKey = Measure.TIMESTAMP;
	protected Object endTimestampKey = Measure.END_TIMESTAMP;
	
	protected SampleSchema schema;
	protected SampleFactory factory;
	
	@Override
	@SuppressWarnings("unchecked")
	public T attach(MeteringDriver metering) {
		factory = createFactory(metering.getSchema());
		return (T)this;
	}

	protected SampleFactory createFactory(SampleSchema rootSchema) {
		SampleSchema ss = rootSchema.createDerivedScheme();
		for(Object key: statics.keySet()) {
			ss.setStatic(key, statics.get(key));
		}
		// TODO do not declare unneeded dynamics
		if (measureKey != null) {
			ss.declareDynamic(measureKey, double.class);
		}
		if (timestampKey != null) {
			ss.declareDynamic(timestampKey, double.class);
		}
		if (endTimestampKey != null) {
			ss.declareDynamic(endTimestampKey, double.class);
		}
		schema = ss;
		
		return ss.createFactory();
	}

	public void setStatic(Object key, Object value) {
		statics.put(key, value);
	}
	
	protected Object getMeasureKey() {
		return measureKey;
	}

	protected void setMeasureKey(Object measureKey) {
		this.measureKey = measureKey;
	}

	protected Object getTimestampKey() {
		return timestampKey;
	}

	protected void setTimestampKey(Object timestampKey) {
		this.timestampKey = timestampKey;
	}

	protected Object getEndTimestampKey() {
		return endTimestampKey;
	}

	protected void setEndTimestampKey(Object endTimestampKey) {
		this.endTimestampKey = endTimestampKey;
	}
}

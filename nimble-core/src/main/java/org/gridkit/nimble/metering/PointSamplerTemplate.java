package org.gridkit.nimble.metering;

import java.io.Serializable;

public class PointSamplerTemplate extends AbstractSamplerTemplate<PointSampler> implements PointSampler, Serializable {

	private static final long serialVersionUID = 20121023L;

	@Override
	public void write(double value, double timestampS) {
		factory.newSample()
			.set(timestampKey, timestampS)
			.set(measureKey, value)
			.submit();
	}

	@Override
	public Object getMeasureKey() {
		return super.getMeasureKey();
	}

	@Override
	public void setMeasureKey(Object measureKey) {
		super.setMeasureKey(measureKey);
	}

	@Override
	public Object getTimestampKey() {
		return super.getTimestampKey();
	}

	@Override
	public void setTimestampKey(Object timestampKey) {
		super.setTimestampKey(timestampKey);
	}
}

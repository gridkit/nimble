package org.gridkit.nimble.metering;

import java.io.Serializable;

public class ScalarSamplerTemplate extends AbstractSamplerTemplate<ScalarSampler> implements ScalarSampler, Serializable {

	private static final long serialVersionUID = 20121023L;

	@Override
	public void write(double value) {
		factory.newSample()
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
}

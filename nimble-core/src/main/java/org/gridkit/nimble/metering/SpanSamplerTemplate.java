package org.gridkit.nimble.metering;

import java.io.Serializable;

public class SpanSamplerTemplate extends AbstractSamplerTemplate<SpanSampler> implements SpanSampler, Serializable {

	private static final long serialVersionUID = 20121023L;

	@Override
	public void write(double value, double timestampS, double durationS) {
		factory.newSample()
			.set(timestampKey, timestampS)
			.set(endTimestampKey, timestampS + durationS)
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

	@Override
	public Object getEndTimestampKey() {
		return super.getEndTimestampKey();
	}

	@Override
	public void setEndTimestampKey(Object endTimestampKey) {
		super.setEndTimestampKey(endTimestampKey);
	}
}

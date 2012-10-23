package org.gridkit.nimble.metering;

import java.io.Serializable;

import org.gridkit.nimble.statistics.TimeUtils;

public class SpanSamplerTemplate extends AbstractSamplerTemplate<SpanSampler> implements SpanSampler, Serializable {

	private static final long serialVersionUID = 20121023L;

	
	@Override
	public void write(double value, long nanoStart, long nanoFinish) {
		factory.newSample()
			.set(timestampKey, TimeUtils.normalize(nanoStart))
			.set(endTimestampKey, TimeUtils.normalize(nanoFinish))
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

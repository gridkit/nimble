package org.gridkit.nimble.monitoring;

import java.io.Serializable;

import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.probe.probe.SchemaConfigurer;

public class NoSchema<V> implements SchemaConfigurer<V>, Serializable {

	private static final long serialVersionUID = 20121114L;

	@Override
	public SampleSchema configure(V target, SampleSchema root) {
		return root;
	}
}

package org.gridkit.nimble.probe.jmx;

public class JmxProbeFactory {

	public static JmxThreadProbe newThreadProbe() {
		return new JmxThreadProbeImpl(null, 1000);
	}

	public static JmxThreadProbe newThreadProbe(MBeanConnector connector) {
		return new JmxThreadProbeImpl(connector, 1000);
	}
	
}

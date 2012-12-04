package org.gridkit.nimble.monitoring;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.gridkit.nimble.pivot.SampleExtractor;
import org.gridkit.nimble.pivot.display.DisplayComponent;
import org.gridkit.nimble.pivot.display.PrintConfig;

public abstract class AbstractMonitoringBundle implements MonitoringBundle {

	private static AtomicInteger COUNTER = new AtomicInteger();
	
	protected final int id = COUNTER.incrementAndGet(); 
	protected final String namespace;
	protected PrintConfig.Recorder printConfig = new Recorder();
	protected List<Object> groupping = new ArrayList<Object>();

	protected AbstractMonitoringBundle(String namespace) {
		this.namespace = namespace;
	}
	
	protected Object getProducerId() {
		return getClass().getSimpleName() + "#" + id;
	}

	@Override
	public void add(String pattern, DisplayComponent component) {
		printConfig.add(namespace + "." + pattern, component);
	}

	@Override
	public void add(DisplayComponent component) {
		printConfig.add(namespace, component);
	}

	@Override
	public void sortByColumn(String... colName) {
		printConfig.sortByColumn(colName);
	}

	@Override
	public void sortBy(SampleExtractor extractor) {
		printConfig.sortBy(extractor);
	}
	
	public void sortByField(Object... keys) {
		printConfig.sortByField(keys);
	}
	
	public void groupBy(Object attr) {
		groupping.add(attr);
	}
	
}

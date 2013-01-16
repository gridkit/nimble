package org.gridkit.lab.monitoring.probe;

import java.util.Collection;

public interface TargetLocator<T> {
	
	public Collection<T> findTargets();

}

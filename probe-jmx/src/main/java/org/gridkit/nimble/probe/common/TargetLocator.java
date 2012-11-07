package org.gridkit.nimble.probe.common;

import java.util.Collection;

public interface TargetLocator<T> {
	
	public Collection<T> findTargets();

}

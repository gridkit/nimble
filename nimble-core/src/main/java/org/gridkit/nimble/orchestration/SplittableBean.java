package org.gridkit.nimble.orchestration;

import java.util.Collection;

import org.gridkit.vicluster.ViNode;

public interface SplittableBean {
	
	public Object getSplit(ViNode target, Collection<ViNode> allTargets);

}

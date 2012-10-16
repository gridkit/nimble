package org.gridkit.nimble.orchestration;

import java.util.Collection;
import java.util.concurrent.Future;

import org.gridkit.vicluster.ViNode;

interface TargetAction {
	
	public Future<Void> submit(ViNode target, Collection<ViNode> allTargets, TargetContext context);

}

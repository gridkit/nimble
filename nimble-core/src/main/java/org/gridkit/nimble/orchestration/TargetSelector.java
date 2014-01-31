package org.gridkit.nimble.orchestration;

import java.util.Collection;

import org.gridkit.nanocloud.Cloud;
import org.gridkit.vicluster.ViNode;

interface TargetSelector {

	public Collection<ViNode> selectTargets(Cloud nodes);
	
}

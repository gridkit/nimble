package org.gridkit.nimble.orchestration;

import java.util.Collection;

import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeSet;

interface TargetSelector {

	public Collection<ViNode> selectTargets(ViNodeSet nodes);
	
}

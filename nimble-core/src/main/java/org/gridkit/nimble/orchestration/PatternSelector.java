package org.gridkit.nimble.orchestration;

import java.util.Collection;

import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeSet;

class PatternSelector implements TargetSelector {
	
	private final String pattern;

	public PatternSelector(String pattern) {
		this.pattern = pattern;
	}

	@Override
	public Collection<ViNode> selectTargets(ViNodeSet nodes) {
		return nodes.listNodes(pattern);
	}

	@Override
	public String toString() {
		return pattern;
	}
}

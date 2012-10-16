package org.gridkit.nimble.orchestration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeSet;

class CompositeTargetSelector implements TargetSelector {

	private final TargetSelector[] selectors;
	
	public CompositeTargetSelector(TargetSelector... selectors) {
		this.selectors = selectors;
	}

	@Override
	public Collection<ViNode> selectTargets(ViNodeSet nodes) {
		Map<String, ViNode> map = new HashMap<String, ViNode>();
		for(TargetSelector selector: selectors) {
			for(ViNode node: selector.selectTargets(nodes)) {
				String name = node.toString();
				map.put(name, node);
			}
		}
		
		return new ArrayList<ViNode>(map.values());
	}

	@Override
	public String toString() {
		return Arrays.toString(selectors);
	}
}

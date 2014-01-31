package org.gridkit.nimble.orchestration;

import org.gridkit.nanocloud.Cloud;

public interface Scenario {

	public void play(Cloud nodeSet);
	
}

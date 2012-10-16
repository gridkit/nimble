package org.gridkit.nimble.orchestration;

import java.util.List;

import org.gridkit.vicluster.ViNode;

public interface Deployable {

	public DeploymentArtifact createArtifact(ViNode target, DepolymentContext context);
	
	interface DepolymentContext {
		
		List<ViNode> getDeploymentTargets();
		
	}
	
	interface EnvironmentContext {
		
	}
	
	interface DeploymentArtifact {
		
		Object deploy(EnvironmentContext context);
	}
}

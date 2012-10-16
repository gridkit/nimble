package org.gridkit.nimble.orchestration;

import java.util.Collection;
import java.util.List;

import org.gridkit.vicluster.ViNode;

public interface Deployable {

	public DeploymentArtifact createArtifact(ViNode target, DepolymentContext context);
	
	public interface DepolymentContext {
		
		Collection<ViNode> getDeploymentTargets();
		
	}
	
	public interface EnvironmentContext {
		
		
	}
	
	public interface DeploymentArtifact {
		
		Object deploy(EnvironmentContext context);
	}
}

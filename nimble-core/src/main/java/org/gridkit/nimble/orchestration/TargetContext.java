package org.gridkit.nimble.orchestration;

import java.rmi.Remote;

interface TargetContext extends Remote {
	
	public Object getBean(int id);
	
	public void deployBean(int id, Object object);

}

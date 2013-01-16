package org.gridkit.lab.monitoring.probe;

import org.gridkit.util.concurrent.FutureEx;

public interface ProbeHandle {

	public void stop();
	
	public FutureEx<Void> getStopFuture();
	
}

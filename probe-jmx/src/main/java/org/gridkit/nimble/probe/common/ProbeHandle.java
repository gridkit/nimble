package org.gridkit.nimble.probe.common;

import org.gridkit.util.concurrent.FutureEx;

public interface ProbeHandle {

	public void stop();
	
	public FutureEx<Void> getStopFuture();
	
}

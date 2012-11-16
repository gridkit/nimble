package org.gridkit.nimble.monitoring;

import org.gridkit.nimble.orchestration.ScenarioBuilder;
import org.gridkit.nimble.orchestration.TimeLine;
import org.gridkit.nimble.pivot.Pivot;
import org.gridkit.nimble.pivot.display.PrintConfig;

public interface MonitoringBundle extends PrintConfig {

	public String getDescription();
	
	public void configurePivot(Pivot pivot);	
	
	public void configurePrinter(PrintConfig printer);

	public void deploy(ScenarioBuilder sb, ServiceProvider context, TimeLine timeLine);
	
	public interface ServiceProvider {
	
		public <T> T lookup(Class<T> service);
		
	}	
}

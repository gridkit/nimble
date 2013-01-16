package org.gridkit.nimble.probe.jmx.threading;

import java.io.Serializable;

import javax.management.MBeanServerConnection;

import org.gridkit.lab.monitoring.probe.PollProbe;
import org.gridkit.lab.monitoring.probe.PollProbeDeployer;
import org.gridkit.lab.monitoring.probe.SamplerProvider;

public class JavaThreadingProbe implements PollProbeDeployer<MBeanServerConnection, JavaThreadStatsSampler>, Serializable {

	private static final long serialVersionUID = 20121017L;

	@Override
	public PollProbe deploy(MBeanServerConnection target, SamplerProvider<MBeanServerConnection, JavaThreadStatsSampler> samplerProvider) {
		JavaThreadStatsSampler sampler = samplerProvider.getSampler(target);
		if (sampler == null) {
			return null;
		}
		ThreadTracker tracker = new ThreadTracker(target, sampler);
				
		return tracker;
	}
	
	private static class ThreadTracker implements PollProbe {
		
		final MBeanServerConnection connection;
		final JavaThreadStatsSampler sampler;
		final JmxThreadTracker tracker;
		
		public ThreadTracker(MBeanServerConnection connection, JavaThreadStatsSampler sampler) {
			this.connection = connection;
			this.tracker = new JmxThreadTracker(connection);
			this.sampler = sampler;
			
			tracker.updateSnapshot();
		}

		@Override
		public void poll() {
			try {
				// validate connection
				connection.getMBeanCount();
			}
			catch(Exception e) {
				return;
			}
			tracker.updateSnapshot();
			
			for(JmxThreadTracker.TradeDetails thread: tracker.getAllThreads()) {
				sampler.report(
					thread.getPrevTimestamp(), 
					thread.getLastTimestamp(), 
					thread.getThreadId(),
					thread.getThreadName(), 
					thread.getCpuTime(), 
					thread.getUserTime(), 
					thread.getBlockedTime(), 
					thread.getBlockedCount(), 
					thread.getWaitTime(), 
					thread.getWaitCount(), 
					thread.getAllocatedBytes());
			}			
		}

		@Override
		public void stop() {
			// do nothing			
		}
	}
}

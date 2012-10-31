package org.gridkit.nimble.probe.jmx;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import javax.management.MBeanServerConnection;

import org.gridkit.nimble.driver.Activity;
import org.gridkit.nimble.driver.MeteringSink;

class JmxThreadProbeImpl extends TimerTask implements JmxThreadProbe, Serializable {

	private static final long serialVersionUID = 20121017L;
	
	private MBeanConnector connector;
	private long pollInterval;

	private Timer timer;
	private ConnectionInfo[] connections;	
	private int samplerCount;
	private int liveSamplers;
	
	public JmxThreadProbeImpl(MBeanConnector connector, long pollInterval) {
		this.connector = connector;
		this.pollInterval = pollInterval;
	}
	
	@Override
	public synchronized void setMBeanConnector(MBeanConnector connector) {
		if (samplerCount > 0) {
			throw new IllegalStateException("Should set MBean connector before any of samplers");
		}
		this.connector = connector;
	}
	
	@Override
	public synchronized Activity addSampler(MeteringSink<JmxAwareSamplerProvider<JavaThreadStatsSampler>> samplerProvider) {
		ensureConnection();
		SamplerActivity act = new SamplerActivity(samplerCount);
		for(ConnectionInfo ci: connections) {
			JavaThreadStatsSampler[] samplers = Arrays.copyOf(ci.samplers, samplerCount + 1);
			samplers[samplerCount] = samplerProvider.getSink().getSampler(ci.connection);
			ci.samplers = samplers;
		}
		++samplerCount;
		++liveSamplers;
		return act;
	}
	
	@Override
	public void run() {
		for(ConnectionInfo ci: connections) {
			ci.tracker.updateSnapshot();
			List<JmxThreadTracker.TradeDetails> threads = ci.tracker.getAllThreads();
			for(JavaThreadStatsSampler sampler: ci.samplers) {
				for(JmxThreadTracker.TradeDetails thread: threads) {
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
		}
	}

	synchronized void removeSampler(int id) {
		boolean live = false;
		for(ConnectionInfo ci: connections) {
			live |= ci.samplers[id] != null;
			ci.samplers[id] = null;
		}
		if (live) {
			--liveSamplers;
		}
		if (liveSamplers == 0) {
			diconnect();
		}		
	}

	private void diconnect() {
		timer.cancel();
		connections = null;
		samplerCount = 0;
	}

	private void ensureConnection() {
		if (connections == null) {
			Collection<MBeanServerConnection> conns = connector.connect();
			connections = new ConnectionInfo[conns.size()];
			int n = 0;
			for(MBeanServerConnection c: conns) {
				ConnectionInfo ci = new ConnectionInfo();
				ci.connection = c;
				connections[n++] = ci;
				ci.tracker = new JmxThreadTracker(c);
				ci.samplers = new JavaThreadStatsSampler[0];
			}
			
			timer = new Timer("JmxThreadProbe[" + connector.toString() + "]", true);
			timer.schedule(this, 0, pollInterval);
		}
	}


	private class SamplerActivity implements Activity {
		
		private final CountDownLatch done = new CountDownLatch(1);
		private final int samplerId;
		
		public SamplerActivity(int samplerId) {
			this.samplerId = samplerId;
		}

		@Override
		public void stop() {
			removeSampler(samplerId);
			done.countDown();
		}

		@Override
		public void join() {
			try {
				done.await();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}


	private static class ConnectionInfo {
		
		MBeanServerConnection connection;
		JmxThreadTracker tracker;
		volatile JavaThreadStatsSampler[] samplers;
		
	}
}

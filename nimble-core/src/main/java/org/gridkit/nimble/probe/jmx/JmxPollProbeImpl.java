package org.gridkit.nimble.probe.jmx;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import javax.management.MBeanServerConnection;

import org.gridkit.nimble.driver.Activity;
import org.gridkit.nimble.driver.MeteringSink;

class JmxPollProbeImpl extends TimerTask implements JmxPollProbe, Serializable {

	private static final long serialVersionUID = 20121017L;
	
	private MBeanConnector connector;
	private long pollInterval;

	private Timer timer;
	private ConnectionInfo[] connections;	
	private int samplerCount;
	private int liveSamplers;
	
	public JmxPollProbeImpl(MBeanConnector connector, long pollInterval) {
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
	public synchronized Activity addSampler(MeteringSink<JmxAwareSamplerProvider<Runnable>> samplerProvider) {
		ensureConnection();
		SamplerActivity act = new SamplerActivity(samplerCount);
		for(ConnectionInfo ci: connections) {
			ci.samplers = Arrays.copyOf(ci.samplers, samplerCount + 1);
			ci.samplers[samplerCount] = samplerProvider.getSink().getSampler(ci.connection);
		}
		++samplerCount;
		++liveSamplers;
		return act;
	}
	
	@Override
	public void run() {
		for(ConnectionInfo ci: connections) {
			for(Runnable sampler: ci.samplers) {
				try {
					sampler.run();
				}
				catch(Exception e) {
					// TODO logging
					// ignore
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
				ci.samplers = new Runnable[0];
			}
			
			timer = new Timer("JmxPollProbe[" + connector.toString() + "]", true);
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
		Runnable[] samplers;
		
	}
}

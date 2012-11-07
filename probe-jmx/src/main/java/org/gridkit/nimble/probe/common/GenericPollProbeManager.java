package org.gridkit.nimble.probe.common;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.gridkit.util.concurrent.DelegatingTaskService;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.util.concurrent.RecuringTask;
import org.gridkit.util.concurrent.SensibleTaskService;
import org.gridkit.util.concurrent.TaskService;
import org.gridkit.util.concurrent.TaskService.Task;

public class GenericPollProbeManager {

	private final TaskService taskService;

	public GenericPollProbeManager() {
		this(SensibleTaskService.getShareInstance());
	}
	
	public GenericPollProbeManager(TaskService taskService) {
		this.taskService = taskService;
	}

	public <T, S> ProbeHandle deploy(final TargetLocator<T> locator, final PollProbeDeployer<T, S> factory, final SamplerProvider<T, S> samplerProvider, final long periodMs) {
		String name = factory.toString() + " @ " + locator.toString();
		final PollGroup group = new PollGroup(name, taskService);
		taskService.schedule(new Task() {
			
			@Override
			public void run() {
				group.deploy(locator, factory, samplerProvider, periodMs);				
			}
			
			@Override
			public void interrupt(Thread taskThread) {
				// do nothing				
			}
			
			@Override
			public void cancled() {
				// do nothing;				
			}
		});
		
		return group;
	}
	
	private static class PollGroup implements ProbeHandle {

		private final String name;
		private final TaskService.Component service;
		private final FutureBox<Void> stop = new FutureBox<Void>();
		
		public PollGroup(String name, TaskService service) {
			this.name = name;
			this.service = new DelegatingTaskService(service);
		}
		
		public <T, S> void deploy(TargetLocator<T> locator, PollProbeDeployer<T, S> factory, SamplerProvider<T, S> sampleProvider, long periodMs) {
			Collection<T> targets = locator.findTargets();
			for(T target: targets) {
				PollProbe probe = factory.deploy(target, sampleProvider);
				RecuringTask.start(service, new ProbeTask(probe), periodMs, TimeUnit.MILLISECONDS);
			}
		}

		@Override
		public FutureEx<Void> getStopFuture() {
			return stop;
		}

		@Override
		public void stop() {
			service.shutdown();
			try {
				stop.setData(null);
			}
			catch(IllegalStateException e) {
				// ignore
			}
		}
		
		@Override
		public String toString() {
			return name;
		}
	}
	
	public static class ProbeTask implements Task {
		
		private final PollProbe probe;

		public ProbeTask(PollProbe probe) {
			this.probe = probe;
		}

		@Override
		public void run() {
			probe.poll();			
		}

		@Override
		public void interrupt(Thread taskThread) {
			// do nothing
		}

		@Override
		public void cancled() {
			probe.stop();
		}
		
		@Override
		public String toString() {
			return probe.toString();
		}
	}
}

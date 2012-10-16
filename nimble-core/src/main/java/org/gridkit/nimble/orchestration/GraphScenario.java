package org.gridkit.nimble.orchestration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeSet;

class GraphScenario implements Scenario {

	
	private List<Action> actions; 
	
	public GraphScenario(List<Action> actions) {
		this.actions = actions;
	}
	
	@Override
	public void play(ViNodeSet nodeSet) {
		new Run(nodeSet, actions).run();
	}

	interface Action {
		
		public int getId();
		
		public Collection<Action> getDependencies();
		
		/**
		 * @return <code>true</code> is action is hosted on master, e.g. checkpoint
		 */
		public boolean isMasterAction();
		
		public TargetSelector getTargetSelector();
		
		public TargetAction getAction();
		
		public String toString();
		
	}	

	private static class Run {
		
		private ViNodeSet nodeSet;
		private Map<ActionId, ActionInfo> actions = new HashMap<GraphScenario.ActionId, GraphScenario.ActionInfo>();
		private Map<String, TargetContext> contexts = new HashMap<String, TargetContext>();
		private Set<ActionId> pending = new HashSet<GraphScenario.ActionId>();		

		public Run(ViNodeSet nodeSet, List<Action> actions) {
			this.nodeSet = nodeSet;
			for(Action action: actions) {
				
				if (action.isMasterAction()) {
					ActionInfo ai = new ActionInfo();
					ai.id = action.getId();
					ai.action = action.getAction();
					ActionId id = new ActionId(ai.id, null);
					this.actions.put(id, ai);
				}
				else {
					Collection<ViNode> targets = action.getTargetSelector().selectTargets(nodeSet);
					if (targets.isEmpty()) {
						throw new IllegalArgumentException("Empty target list: " + action.getTargetSelector());
					}
					for(ViNode node: targets) {
						String name = node.toString();
						ActionId id = new ActionId(action.getId(), name);
						ActionInfo ai = new ActionInfo();
						ai.id = action.getId();
						ai.allTargets = targets;
						ai.nodeName = name;
						ai.target = node;
						this.actions.put(id, ai);
						ai.dependencies.add(ensureIntialized(name));
					}
				}
			}
			
			// generating dependencies
			for(ActionInfo actionInfo: this.actions.values()) {
				Action action = actions.get(actionInfo.id);
				if (action.getId() != actionInfo.id) {
					throw new IllegalArgumentException("Broned action list");
				}
				for(Action dep: action.getDependencies()) {
					if (action.isMasterAction()) {
						actionInfo.dependencies.addAll(findAll(dep.getId()));
					}
					else {
						ActionId local = new ActionId(dep.getId(), actionInfo.nodeName);
						ActionId global = new ActionId(dep.getId(), null);
						if (this.actions.containsKey(local)) {
							actionInfo.dependencies.add(local);
						}
						else if (this.actions.containsKey(global)) {
							actionInfo.dependencies.add(global);
						}
						else {
							throw new IllegalArgumentException("Action dependency should be eigther local or global");
						}						
					}					
				}
			}
			pending = new HashSet<ActionId>(this.actions.keySet());
		}

		public void run() {
			while(!isCompleted()) {
				if (!dispatch()) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		
		public synchronized boolean isCompleted() {
			return pending.isEmpty();
		}
		
		public synchronized boolean dispatch() {
			boolean noop = true;
			restart:
			while(true) {
				for(ActionId id: pending) {
					ActionInfo ai = actions.get(id);
					if (readyToRun(ai)) {
						fire(ai);
						noop = false;
						continue restart;
					}
				}
				break;
			}
			return noop;
		}
		
		private boolean readyToRun(ActionInfo ai) {
			if (ai.pending != null) {
				// already started
				return false;				
			}
			Iterator<ActionId> it = ai.dependencies.iterator();
			while(it.hasNext()) {
				if (isCompleted(it.next())) {
					it.remove();
				}
			}
			return ai.dependencies.isEmpty();
		}
		
		private boolean isCompleted(ActionId id) {
			if (!pending.contains(id)) {
				return true;
			}
			else {
				ActionInfo ai = actions.get(id);
				if (ai.pending == null) {
					return false;
				}
				else {
					if (ai.pending.isDone()) {
						complete(ai);
						return true;
					}
					else {
						return false;
					}
				}
			}
		}

		@SuppressWarnings("rawtypes")
		private void complete(ActionInfo ai) {
			try {
				ai.pending.get();
				if (ai.target instanceof InitContextAction) {
					TargetContext ctx = (TargetContext) ((Future)ai.pending).get();
					contexts.put(ai.nodeName, ctx);
				}
				pending.remove(new ActionId(ai.id, ai.nodeName));
				ai.pending = null;
			}
			catch(ExecutionException e) {
				throw new RuntimeException("Run failed: " + e.getCause());
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		private void fire(ActionInfo ai) {
			TargetContext ctx = contexts.get(ai.nodeName);
			Future<Void> result = ai.action.submit(ai.target, ai.allTargets, ctx);
			ai.pending = result;
			if (result.isDone()) {
				complete(ai);
			}
		}

		private Collection<ActionId> findAll(int id) {
			List<ActionId> result = new ArrayList<ActionId>();
			for(ActionId aid: actions.keySet()) {
				if (aid.action == id) {
					result.add(aid);
				}
			}
			return result;
		}

		private ActionId ensureIntialized(String name) {
			ActionId id = new ActionId(-1, name);
			if (!actions.containsKey(id)) {
				ActionInfo ai = new ActionInfo();
				ai.action = new InitContextAction();
				ai.nodeName = name;
				ai.target = nodeSet.node(name);
				actions.put(id, ai);
			}
			return id;
		}
		
	}
	
	private static class ActionId {
		
		final int action;
		final String target;
		
		public ActionId(int action, String target) {
			this.action = action;
			this.target = target;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + action;
			result = prime * result
					+ ((target == null) ? 0 : target.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ActionId other = (ActionId) obj;
			if (action != other.action)
				return false;
			if (target == null) {
				if (other.target != null)
					return false;
			} else if (!target.equals(other.target))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return "[" + (action == -1 ? "init" : String.valueOf(action)) + "]@" + target;
		}		
	}
	
	private static class ActionInfo {
		
		int id;		
		String nodeName;
		ViNode target;
		Collection<ViNode> allTargets;
		TargetAction action;		
		Future<Void> pending;
		Set<ActionId> dependencies = new HashSet<ActionId>();
	}
	
	private static class InitContextAction implements TargetAction {

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public Future<Void> submit(ViNode target, Collection<ViNode> allTargets, TargetContext context) {
			return (Future)target.exec(new Callable<TargetContext>() {
				@Override
				public SimpleTargetContext call() throws Exception {
					return new SimpleTargetContext();
				}
			});
		}		
	}
	
	private static class SimpleTargetContext implements TargetContext {
		
		private Map<Integer, Object> beans = new HashMap<Integer, Object>();

		@Override
		public synchronized Object getBean(int id) {
			return beans.get(id);
		}

		@Override
		public synchronized void deployBean(int id, Object object) {
			if (beans.containsKey(id)) {
				throw new IllegalArgumentException("Bean [" + id + "] is already defined, cannot deploy " + object);
			}
			else {
				beans.put(id, object);
			}
		}
	}
}

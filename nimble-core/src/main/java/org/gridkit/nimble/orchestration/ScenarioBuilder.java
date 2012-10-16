package org.gridkit.nimble.orchestration;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gridkit.vicluster.ViNodeSet;

public class ScenarioBuilder {

	public static final String START = "START"; 
	
	private Map<String, CheckPointAction> namedCheckpoints = new HashMap<String, CheckPointAction>();
	
	private IdentityHashMap<Object, Bean> beans = new IdentityHashMap<Object, ScenarioBuilder.Bean>();
	private IdentityHashMap<Object, Bean> stubs = new IdentityHashMap<Object, ScenarioBuilder.Bean>();
	
	private List<Bean> beanList = new ArrayList<Bean>();
	private List<Action> actionList = new ArrayList<Action>();
	
	private Tracker tracker;
	private List<Action> run = new ArrayList<Action>();
	private Order order = Order.NATURAL;
	
	public ScenarioBuilder() {
		start();
	}
	
	void start() {
		CheckPointAction start = new CheckPointAction(START);
		tracker = new NaturalDependencyTracker(start);
	}
	
	@SuppressWarnings("unchecked")
	public <V> V deploy(V bean) {
		if (beans.containsKey(bean)) {
			throw new IllegalArgumentException("Bean [" + bean + "] is already deployed");
		}
		if (stubs.containsKey(bean)) {
			throw new IllegalArgumentException("Cannot deploy stub");
		}
		else {
			Bean beanMeta = newBeanInfo(bean.getClass().getInterfaces());
			beanMeta.reference = bean;
			beans.put(bean, beanMeta);
			addDeployAction(beanMeta, bean);
			return (V) beanMeta.proxy;
		}
	}

	private Bean newBeanInfo(Class<?>[] interfaces) {
		Bean beanMeta = new Bean(beanList.size(), interfaces);
		beanList.add(beanMeta);
		Object proxy = newProxy(beanMeta);
		beanMeta.proxy = proxy;
		stubs.put(proxy, beanMeta);
		return beanMeta;
	}
	
	public void natural() {	
		if (order != Order.NATURAL) {
			CheckPointAction sync = makeSync();
			tracker = new NaturalDependencyTracker(sync);
		}		
	}
	
	public void sequential() {		
		if (order != Order.SEQUENTIAL) {
			CheckPointAction sync = makeSync();
			tracker = new SequentialTracker(sync);
		}		
	}
	
	public void sync() {
		CheckPointAction sync = makeSync();
		restartTracker(sync);
	}

	public void checkpoint(String name) {
		CheckPointAction sync = makeCheckpoint(name);
		restartTracker(sync);		
	}
	
	public void from(String name) {
		CheckPointAction cp = namedCheckpoints.get(name);
		if (cp == null) {
			throw new IllegalArgumentException("Checkpoint (" + name + ") is not defined");
		}
		run.clear();
		restartTracker(cp);		
	}

	public void fromStart() {
		from(START);
	}
	
	public void join(String name) {
		CheckPointAction cp = namedCheckpoints.get(name);
		if (cp == null) {
			throw new IllegalArgumentException("Checkpoint (" + name + ") is not defined");
		}
		CheckPointAction sync = makeSync();
		cp.addDependency(sync);
		from(name);
	}
	
	private void restartTracker(CheckPointAction sync) {
		if (order == Order.NATURAL) {
			tracker = new NaturalDependencyTracker(sync);			
		}
		else {
			tracker = new SequentialTracker(sync);
		}
	}
	
	private CheckPointAction makeSync() {
		CheckPointAction sync = new CheckPointAction();
		for(Action action: run) {
			sync.addDependency(action);
		}
		run.clear();
		return sync;
	}
	
	private CheckPointAction makeCheckpoint(String name) {
		CheckPointAction sync = new CheckPointAction(name);
		for(Action action: run) {
			sync.addDependency(action);
		}
		run.clear();
		return sync;		
	}
	
	public void open(String scenarioName) {
		
	}
	
	public void close(String scenarioName) {
	}
	
	public Scenario getScenario() {
		return new PrintScenario();
	}
	
	private Object newProxy(Bean beanMeta) {
		Stub stub = new Stub(beanMeta);
		return Proxy.newProxyInstance(Stub.class.getClassLoader(), beanMeta.interfaces, stub);
	}

	private Bean declareDeployable(Method method) {
		Class<?> rt = method.getReturnType();
		if (rt.getInterfaces().length > 0) {
			return newBeanInfo(rt.getInterfaces());
		}
		else {
			return null;
		}
	}
	
	private void addDeployAction(Bean beanInfo, Object proto) {
		Deployable deployable = new DeployablePrototype(proto); 
		DeployAction action = new DeployAction(beanInfo, deployable);
		addAction(action);
		beanInfo.deployAction = action;
	}
	
	private void addCallAction(Bean beanInfo, Method method, Object[] args, Bean deployTo) {
		Object[] refinedArgs = args == null ? new Object[0] : refine(args);
		CallAction action = new CallAction(beanInfo, method, refinedArgs, deployTo);
		addAction(action);
		if (deployTo != null) {
			deployTo.deployAction = action;
		}
	}
	
	private Object[] refine(Object[] args) {
		Object[] copy = Arrays.copyOf(args, args.length);
		for(int i = 0; i != copy.length; ++i) {
			if (stubs.containsKey(copy[i])) {
				copy[i] = stubs.get(copy[i]);
			}
			if (beans.containsKey(copy[i])) {
				copy[i] = beans.get(copy[i]);
			}
		}
		return copy;
	}

	private void addAction(Action action) {
		run.add(action);
		tracker.actionAdded(action, true);
	}
	
	private static class Bean implements Serializable {
		
		final int id;
		final Class<?>[] interfaces;
		
		transient Object reference;
		transient Object proxy;
		transient Action deployAction;
		
		public Bean(int id, Class<?>[] interfaces) {
			this.id = id;
			this.interfaces = interfaces;
		}
	}
	
	enum Order {
		SEQUENTIAL,
		NATURAL
	}
	
	abstract class Tracker {
		
		abstract void actionAdded(Action action, boolean active);
	}
	
	class NaturalDependencyTracker extends Tracker {
		
		Action checkPoint;
		Map<Integer, Action> casualOrder = new HashMap<Integer, Action>();
		
		public NaturalDependencyTracker(Action checkPoint) {
			this.checkPoint = checkPoint;
		}
		
		@Override
		void actionAdded(Action action, boolean active) {
			if (active) {
				action.addDependency(checkPoint);
				for(Bean bean: action.getRelatedBeans()) {
					if (casualOrder.containsKey(bean.id)) {
						action.addDependency(casualOrder.get(bean.id));
					}
				}
			}
			for(Bean bean: action.getAffectedBeans()) {
				casualOrder.put(bean.id, action);
			}
		}
	}

	class SequentialTracker extends Tracker {
		
		private Action prevAction;
		
		public SequentialTracker(Action prevAction) {
			this.prevAction = prevAction;
		}

		
		@Override
		void actionAdded(Action action, boolean active) {
			if (prevAction != null && active) {
				action.addDependency(prevAction);
			}
			prevAction = action;
		}		
	}
	
	abstract class Action {

		int actionId = actionList.size();
		{ actionList.add(this); };
		
		List<Action> required = new ArrayList<Action>();
		
		public abstract List<Bean> getRelatedBeans();
		public abstract List<Bean> getAffectedBeans();

		public List<Action> getDependencies() {
			return required;
		}
		
		public void addDependency(Action action) {
			required.add(action);
		}
		
	}
	
	class DeployAction extends Action {
		
		private final Bean beanInfo;
		private final Deployable deployable;

		public DeployAction(Bean beanInfo, Deployable deployable) {
			this.beanInfo = beanInfo;
			this.deployable = deployable;
		}

		@Override
		public List<Bean> getRelatedBeans() {
			return Collections.singletonList(beanInfo);
		}
		
		@Override
		public List<Bean> getAffectedBeans() {
			return Collections.singletonList(beanInfo);
		}
		
		@Override
		public String toString() {
			return "[" + actionId + "] DEPLOY: " + deployable;
		}		
	}
	
	class CallAction extends Action {
		
		private final Bean bean;
		private final Method method;
		private final Object[] arguments;
		private final Bean deployTarget;
		private final List<Bean> related = new ArrayList<ScenarioBuilder.Bean>();
		
		public CallAction(Bean bean, Method method, Object[] arguments, Bean deployTarget) {
			this.bean = bean;
			this.method = method;
			this.arguments = arguments;
			this.deployTarget = deployTarget;
			
			related.add(bean);
			if (deployTarget != null) {
				related.add(deployTarget);
			}
			for(Object o: arguments) {
				if (o instanceof Bean) {
					related.add((Bean) o);
				}
			}
			
			addDependency(bean.deployAction);
		}

		@Override
		public List<Bean> getRelatedBeans() {
			return related;
		}

		@Override
		public List<Bean> getAffectedBeans() {
			if (deployTarget != null) {
				return Collections.singletonList(deployTarget);
			}
			else {
				return Collections.emptyList();
			}
		}

		@Override
		public String toString() {
			return "[" + actionId + "] CALL: " + method.getDeclaringClass().getSimpleName() + "::" +method.getName();
		}
	}
	
	class CheckPointAction extends Action {
		
		private final String name;

		public CheckPointAction() {
			this.name = "<sync>";
		}		
		
		public CheckPointAction(String name) {
			this.name = name;
			if (namedCheckpoints.containsKey(name)) {
				throw new IllegalArgumentException("Checkpoint (" + name + ") is already defined");
			}
			namedCheckpoints.put(name, this);
		}

		@Override
		public List<Bean> getRelatedBeans() {
			return Collections.emptyList();
		}

		@Override
		public List<Bean> getAffectedBeans() {
			return Collections.emptyList();
		}
		
		@Override
		public String toString() {
			return "[" + actionId + "] (" + name + ")";
		}
	}
	
	class DeployablePrototype implements Deployable {
		
		private final Object prototype;

		public DeployablePrototype(Object prototype) {
			this.prototype = prototype;
		}

		@Override
		public Object deploy(DeploymentScope scope) {
			return prototype;
		}
		
		@Override
		public String toString() {
			return prototype.toString();
		}
	}
	
	class Stub implements InvocationHandler, Serializable {

		private final Bean bean;
		
		public Stub(Bean bean) {
			this.bean = bean;
		}
		
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			
			if (method.getDeclaringClass() == Object.class) {
				return method.invoke(this, args);
			}
			else {
				Bean rbean = declareDeployable(method);
				addCallAction(bean, method, args, rbean);
				
				return rbean == null ? null : rbean.proxy;
			}
		}
	}
	
	interface Deployable {
		
		public Object deploy(DeploymentScope scope);
		
	}
	
	interface DeploymentScope {
		
	}
	
	private class PrintScenario implements Scenario {

		private Map<Integer, Action> pending = new HashMap<Integer, ScenarioBuilder.Action>();
		private Map<Integer, ExecutionInfo> execStatus = new HashMap<Integer, ExecutionInfo>();
		private List<Action> actionList = new ArrayList<ScenarioBuilder.Action>();
		
		@Override
		public void play(ViNodeSet nodeSet) {
			start();
			while(!pending.isEmpty()) {
				findActionable();
				perform();
			}
		}

		private void start() {
			for(Action action: ScenarioBuilder.this.actionList) {
				pending.put(action.actionId, action);				
			}
		}
		
		private void findActionable() {
			nextAction:
			for(Action action: pending.values()) {
				for(Action dep: action.getDependencies()) {
					if (!isCompleted(dep.actionId)) {
						continue nextAction;
					}
				}
				actionList.add(action);
			}			
		}

		private void perform() {
			System.out.println("Action list:");
			if (actionList.isEmpty()) {
				throw new RuntimeException("Dead lock");
			}
			for(Action action: actionList) {
				System.out.println("  " + action);
				execStatus.put(action.actionId, new ExecutionInfo());
				pending.remove(action.actionId);
			}
			for(ExecutionInfo ei: execStatus.values()) {
				if (!ei.pending.isEmpty()) {
					Iterator<Integer> it = ei.pending.iterator();
					while(it.hasNext()) {
						if (isCompleted(it.next())) {
							it.remove();
						}
					}
				}
			}
			actionList.clear();
			System.out.println("");
		}
		
		private boolean isCompleted(Integer id) {
			ExecutionInfo ei = execStatus.get(id);
			return ei != null && ei.pending.isEmpty();
		}
	}
	
	private static class ExecutionInfo {
		
		private int actionId;
		private List<Integer> pending = new ArrayList<Integer>();
		
	}
}

package org.gridkit.nimble.orchestration;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gridkit.vicluster.ViNodeSet;

public class ScenarioBuilder {

	private boolean open;
	private boolean completed;
	
	private Map<String, Flow> namedScopes = new HashMap<String, ScenarioBuilder.Flow>();
	
	private Flow rootScope;
	private Flow currentNamed;
	private Flow current;
	
	private IdentityHashMap<Object, Bean> beans = new IdentityHashMap<Object, ScenarioBuilder.Bean>();
	private IdentityHashMap<Object, Bean> stubs = new IdentityHashMap<Object, ScenarioBuilder.Bean>();
	
	private int idCounter = 1;
	private int actionCounter = 1;
	
	public void start() {
		if (rootScope == null) {
			rootScope = new SequentialFlow();
			currentNamed = rootScope;
			current = rootScope;
		}
		else {
			throw new IllegalStateException("You can start scenarion only once");
		}
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
		Bean beanMeta = new Bean(idCounter++, interfaces);
		Object proxy = newProxy(beanMeta);
		beanMeta.proxy = proxy;
		stubs.put(proxy, beanMeta);
		return beanMeta;
	}
	
	public void subflow(String name) {
		if (name == null || name.length() == 0) {
			throw new IllegalArgumentException("Scenario name schuld not be empty");
		}
		else if (namedScopes.containsKey(name)) {
			throw new IllegalArgumentException("Duplicate scenario name '" + name + "'");
		}
		else {
			SequentialFlow flow = new SequentialFlow(name);
			flow.parentFlow = currentNamed;
			namedScopes.put(name, flow);
			SubflowAction action = new SubflowAction(flow);
			addAction(action);			
		}		
	}
	
	public void parallel() {		
	}
	
	public void natural() {	
		if (current.getOrder() != Order.NATURAL) {
			NaturalFlow flow = new NaturalFlow();
			flow.parentFlow = currentNamed;
			currentNamed.addAction(new SubflowAction(flow));
			current = flow;
		}
	}
	
	public void sequential() {		
	}
	
	public void open(String scenarioName) {
		
	}
	
	public void close(String scenarioName) {
	}
	
	public void finish() {
		if (currentNamed != rootScope) {
			throw new IllegalStateException("finsih() should match start()");
		}
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
	}
	
	private void addCallAction(Bean beanInfo, Method method, Object[] args, Bean deployTo) {
		Object[] refinedArgs = args == null ? new Object[0] : refine(args);
		CallAction action = new CallAction(beanInfo, method, refinedArgs, deployTo);
		addAction(action);
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
		current.addAction(action);
	}
	
	private static class Bean implements Serializable {
		
		final int id;
		final Class<?>[] interfaces;
		
		transient Object reference;
		transient Object proxy;
		transient int deployAction;
		
		public Bean(int id, Class<?>[] interfaces) {
			this.id = id;
			this.interfaces = interfaces;
		}
	}

	enum Order {
		SEQUENTIAL,
		NATURAL,
		PARALLEL
	}
	
	abstract class Flow {
		
		String id;
		Flow parentFlow;
		
		List<Action> actions = new ArrayList<Action>();
		
		public abstract Order getOrder();
		
		public abstract void addAction(Action action);
		
	}
	
	class ParallelFlow extends Flow {
		
		public ParallelFlow() {			
		}
		
		public ParallelFlow(String name) {
			id = name;
		}

		@Override
		public Order getOrder() {
			return Order.PARALLEL;
		}

		@Override
		public void addAction(Action action) {
			actions.add(action);
		}
	}

	class NaturalFlow extends Flow {
		
		Map<Integer, Integer> casualOrder = new HashMap<Integer, Integer>();
		
		public NaturalFlow() {			
		}
		
		public NaturalFlow(String name) {
			id = name;
		}

		@Override
		public Order getOrder() {
			return Order.PARALLEL;
		}

		@Override
		public void addAction(Action action) {
			for(Bean bean: action.getRelatedBeans()) {
				if (casualOrder.containsKey(bean.id)) {
					action.required.add(casualOrder.get(bean.id));
				}
			}
			for(Bean bean: action.getAffectedBeans()) {
				casualOrder.put(bean.id, action.actionId);
			}
			actions.add(action);
		}
	}

	class SequentialFlow extends Flow {
		
		private int prevAction = -1;
		
		public SequentialFlow() {
		}
		
		public SequentialFlow(String name) {
			id = name;
		}

		@Override
		public Order getOrder() {
			return Order.SEQUENTIAL;
		}

		@Override
		public void addAction(Action action) {
			if (prevAction >= 0) {
				action.required.add(prevAction);
			}
			prevAction = action.actionId;
			actions.add(action);
		}		
	}
	
	abstract class Action {

		int actionId = actionCounter++;
		List<Integer> required = new ArrayList<Integer>();
		
		public abstract List<Bean> getRelatedBeans();
		public abstract List<Bean> getAffectedBeans();
		
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
			if (deployTarget != null) {
				related.add(deployTarget);
			}
			for(Object o: arguments) {
				if (o instanceof Bean) {
					related.add((Bean) o);
				}
			}
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
	
	class SubflowAction extends Action {
		
		private final Flow flow;
		
		public SubflowAction(Flow flow) {
			this.flow = flow;
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
			return "[" + actionId + "] FLOW: " + flow;
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
			start(rootScope);
			while(!pending.isEmpty()) {
				findActionable();
				perform();
			}
		}

		private void start(Flow flow) {
			for(Action action: flow.actions) {
				pending.put(action.actionId, action);				
			}
		}
		
		private void findActionable() {
			nextAction:
			for(Action action: pending.values()) {
				for(Integer id: action.required) {
					if (!isCompleted(id)) {
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
				if (action instanceof SubflowAction) {
					Flow flow = ((SubflowAction)action).flow;
					ExecutionInfo ei = new ExecutionInfo();
					ei.actionId = action.actionId;
					for(Action sa: flow.actions) {
						ei.pending.add(sa.actionId);
					}
					execStatus.put(action.actionId, ei);
					start(flow);
				}
				else {
					execStatus.put(action.actionId, new ExecutionInfo());
				}
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

package org.gridkit.nimble.orchestration;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.gridkit.nimble.orchestration.Deployable.DeploymentArtifact;
import org.gridkit.nimble.orchestration.Deployable.DepolymentContext;
import org.gridkit.nimble.orchestration.Deployable.EnvironmentContext;
import org.gridkit.util.concurrent.TimedFuture;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeSet;

public class ScenarioBuilder {

	public static final String START = "START"; 
	
	private Map<String, CheckpointAction> namedCheckpoints = new HashMap<String, CheckpointAction>();
	
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
		CheckpointAction start = new CheckpointAction(START);
		tracker = new NaturalDependencyTracker(start);
	}

	public <V> V deploy(V bean) {
		return internalDeploy(null, bean);
	}
	
	public <V> V deploy(String pattern, V bean) {
		return internalDeploy(new PatternSelector(pattern), bean);
	}
	
	@SuppressWarnings("unchecked")
	private <V> V internalDeploy(TargetSelector scope, V bean) {
		if (beans.containsKey(bean)) {
			throw new IllegalArgumentException("Bean [" + bean + "] is already deployed");
		}
		if (stubs.containsKey(bean)) {
			throw new IllegalArgumentException("Cannot deploy stub");
		}
		else {
			Bean beanMeta = newBeanInfo(bean.getClass().getInterfaces());
			beanMeta.reference = bean;
			beanMeta.scope = scope;
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
	
	// to be removed
	@SuppressWarnings("unused")
	private void natural() {	
		if (order != Order.NATURAL) {
			CheckpointAction sync = makeSync();
			tracker = new NaturalDependencyTracker(sync);
		}		
	}
		
	// to be removed
	@SuppressWarnings("unused")
	private void sequential() {		
		if (order != Order.SEQUENTIAL) {
			CheckpointAction sync = makeSync();
			tracker = new SequentialTracker(sync);
		}		
	}
	
	public void sync() {
		CheckpointAction sync = makeSync();
		restartTracker(sync);
	}

	public void sleep(long millis) {
		CheckpointAction sync = makeSleep(millis);
		restartTracker(sync);		
	}
	
	public void checkpoint(String name) {
		CheckpointAction sync = makeCheckpoint(name);
		restartTracker(sync);		
	}
	
	public void from(String name) {
		CheckpointAction cp = namedCheckpoints.get(name);
		if (cp == null) {
			throw new IllegalArgumentException("Checkpoint (" + name + ") is not defined");
		}
		run.clear();
		run.add(cp);
		restartTracker(cp);		
	}

	public void fromStart() {
		from(START);
	}
	
	public void join(String name) {
		CheckpointAction cp = namedCheckpoints.get(name);
		if (cp == null) {
			throw new IllegalArgumentException("Checkpoint (" + name + ") is not defined");
		}
		CheckpointAction sync = makeSync();
		cp.addDependency(sync);
		from(name);
	}
	
	public Scenario getScenario() {
		return new GraphScenario(exportGraph());
	}

	private void restartTracker(CheckpointAction sync) {
		if (order == Order.NATURAL) {
			tracker = new NaturalDependencyTracker(sync);			
		}
		else {
			tracker = new SequentialTracker(sync);
		}
	}
	
	private CheckpointAction makeSync() {
		return makeSleep(0);
	}

	private CheckpointAction makeSleep(long sleep) {
		CheckpointAction sync = new CheckpointAction(sleep);
		for(Action action: run) {
			sync.addDependency(action);
		}
		run.clear();
		run.add(sync);
		return sync;
	}
	
	private CheckpointAction makeCheckpoint(String name) {
		CheckpointAction sync = new CheckpointAction(name);
		for(Action action: run) {
			sync.addDependency(action);
		}
		run.clear();
		run.add(sync);
		return sync;		
	}
	
	void debug_simulate() {
		new PrintScenario().play(null);
	}
	
	private Object newProxy(Bean beanMeta) {
		Stub stub = new Stub(beanMeta, new BeanScope());
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
		Deployable deployable = proto instanceof Deployable ? (Deployable) proto : new DeployablePrototype(proto); 
		DeployAction action = new DeployAction(beanInfo, deployable);
		addAction(action);
		beanInfo.deployAction = action;
	}
	
	private void addCallAction(Bean beanInfo, Scope scope, Method method, Object[] args, Bean deployTo) {
		Object[] refinedArgs = args == null ? new Object[0] : refine(args);
		CallAction action = new CallAction(beanInfo, scope, method, refinedArgs, deployTo);
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
	
	private TargetSelector getBeanScope(Bean bean) {
		if (bean.scope != null) {
			return bean.scope;
		}
		else {
			return deriveScope(bean, bean);
		}
	}
	
	private TargetSelector deriveScope(Bean rootBean, Bean bean) {
		List<TargetSelector> selectors = new ArrayList<TargetSelector>();
		for(Action action: actionList) {
			if (action instanceof CallAction) {
				CallAction ca = (CallAction) action;
				if (ca.isUsing(bean)) {
					TargetSelector ts = ca.bean.scope;
					if (ts == null) {
						if (ca.bean == rootBean) {
							throw new RuntimeException("Cyclic dependency resolving bean scope: " + rootBean.reference);
						}
						else {
							selectors.add(deriveScope(rootBean, ca.bean));
						}
					}
					else {
						selectors.add(ts);
					}
				}
			}
		}
		if (selectors.isEmpty()) {
			throw new RuntimeException("Bean " + bean.reference + " is not used");
		}
		return new CompositeTargetSelector(selectors.toArray(new TargetSelector[selectors.size()]));
	}

	private List<GraphScenario.Action> exportGraph() {
		
		List<GraphScenario.Action> script = new ArrayList<GraphScenario.Action>();
		for(Action action: actionList) {
			script.add(export(script, action));
		}
		
		return script;
	}
	
	private ScriptAction export(List<GraphScenario.Action> list, Action action) {
		
		if (action instanceof DeployAction) {
			DeployAction da = (DeployAction) action;
			DeployExecutor exec = new DeployExecutor(new BeanRef(da.bean.id), da.deployable);
			
			ScriptAction sa = new ScriptAction(list, da.actionId, getBeanScope(da.bean), toArray(da.getDependencies()), exec);
			return sa;			
		}
		else if (action instanceof CallAction) {
			CallAction ca = (CallAction) action;
			TargetSelector scope = ca.scope.getSelector(action);
			CallExecutor exec = new CallExecutor(ca);
			
			ScriptAction sa = new ScriptAction(list, ca.actionId, scope, toArray(ca.getDependencies()), exec);
			return sa;			
		}
		else if (action instanceof CheckpointAction) {
			CheckpointAction ca = (CheckpointAction) action;
			String name = ca.name != null ? ("(" + ca.name + ")") : ca.sleep == 0 ? "<sync #" + ca.actionId + ">" : "<sleep " + ca.sleep + "ms>";
			CheckpointExecutor exec = new CheckpointExecutor(name, ca.sleep);
			ScriptAction sa = new ScriptAction(list, action.actionId, null, toArray(action.getDependencies()), exec);
			return sa;			
		}
		else {
			throw new Error("Imposible");
		}
	}

	private int[] toArray(List<Action> dependencies) {
		int[] deps = new int[dependencies.size()];
		for(int i = 0; i != deps.length; ++i) {
			deps[i] = dependencies.get(i).actionId;
		}
		return deps;
	}

	private static class Bean {
		
		final int id;
		final Class<?>[] interfaces;
		
		TargetSelector scope;
		Object reference;
		Object proxy;
		Action deployAction;
		
		public Bean(int id, Class<?>[] interfaces) {
			this.id = id;
			this.interfaces = interfaces;
		}
	}
	
	private static class BeanRef implements Serializable {

		private static final long serialVersionUID = 20121016L;
		
		final int id;

		public BeanRef(int id) {
			this.id = id;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + id;
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
			BeanRef other = (BeanRef) obj;
			if (id != other.id)
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return "{" + id + "}";
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
		
		private final Bean bean;
		private final Deployable deployable;

		public DeployAction(Bean beanInfo, Deployable deployable) {
			this.bean = beanInfo;
			this.deployable = deployable;
		}

		@Override
		public List<Bean> getRelatedBeans() {
			return Collections.singletonList(bean);
		}
		
		@Override
		public List<Bean> getAffectedBeans() {
			return Collections.singletonList(bean);
		}
		
		@Override
		public String toString() {
			return "[" + actionId + "] DEPLOY: {" + bean.id + "} " + deployable;
		}		
	}
	
	class CallAction extends Action {
		
		private final Bean bean;
		private final Scope scope;
		private final Method method;
		private final Object[] arguments;
		private final Bean deployTarget;
		private final List<Bean> related = new ArrayList<ScenarioBuilder.Bean>();
		
		public CallAction(Bean bean, Scope scope, Method method, Object[] arguments, Bean deployTarget) {
			this.bean = bean;
			this.scope = scope;
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

		public boolean isUsing(Bean bean) {
			for(Object arg: arguments) {
				if (arg == bean) {
					return true;
				}
			}
			return false;
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
	
	class CheckpointAction extends Action {
		
		private final String name;
		private final long sleep;

		public CheckpointAction() {
			this.name = null;
			this.sleep = 0;
		}		

		public CheckpointAction(long sleepMillis) {
			this.name = null;
			this.sleep = sleepMillis;
		}		
		
		public CheckpointAction(String name) {
			this.name = name;
			this.sleep = 0;
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
			return "[" + actionId + "] " 
					+ ( name != null ? "name" : sleep == 0 ? "<sync>" : "<sleep " + sleep + "ms>");
		}
	}
	
	static class DeployablePrototype implements Deployable, DeploymentArtifact, Serializable {
		
		private static final long serialVersionUID = 20121016L;
		
		private final Object prototype;

		public DeployablePrototype(Object prototype) {
			this.prototype = prototype;
		}

		@Override
		public DeploymentArtifact createArtifact(ViNode target, DepolymentContext context) {
			return this;
		}

		@Override
		public Object deploy(EnvironmentContext context) {
			return prototype;
		}

		@Override
		public String toString() {
			return prototype.toString();
		}
	}
	
	abstract class Scope {
		
		abstract TargetSelector getSelector(Action action);
	}

	class MasterScope extends Scope {
		
		TargetSelector getSelector(Action action) {
			return null;
		}
	}

	class BeanScope extends Scope {

		TargetSelector getSelector(Action action) {
			if (action instanceof CallAction) {
				return getBeanScope(((CallAction)action).bean);
			}
			else if (action instanceof DeployAction) {
				return getBeanScope(((DeployAction)action).bean);
			}
			else {
				throw new UnsupportedOperationException();
			}
		}		
	}
	
	class Stub implements InvocationHandler {

		private final Bean bean;
		private final Scope scope;
		
		public Stub(Bean bean, Scope scope) {
			this.bean = bean;
			this.scope = scope;
		}
		
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			
			if (method.getDeclaringClass() == Object.class) {
				return method.invoke(this, args);
			}
			else {
				Bean rbean = declareDeployable(method);
				addCallAction(bean, scope, method, args, rbean);
				
				return rbean == null ? null : rbean.proxy;
			}
		}
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
		
		@SuppressWarnings("unused")
		private int actionId;
		private List<Integer> pending = new ArrayList<Integer>();
		
	}
	
	private static class ScriptAction implements GraphScenario.Action {

		private final List<GraphScenario.Action> allActions;
		
		private final int id;
		private final TargetSelector selector;
		private final int[] dependencies;
		private final TargetAction action;
		
		public ScriptAction(List<GraphScenario.Action> allActions, int id, TargetSelector selector, int[] dependencies, TargetAction action) {
			this.allActions = allActions;
			this.id = id;
			this.selector = selector;
			this.dependencies = dependencies;
			this.action = action;
		}

		@Override
		public int getId() {
			return id;
		}

		@Override
		public Collection<GraphScenario.Action> getDependencies() {
			GraphScenario.Action[] deps = new GraphScenario.Action[dependencies.length];
			for(int i = 0; i != dependencies.length; ++i) {
				deps[i] = allActions.get(dependencies[i]);
			}
			return Arrays.asList(deps);
		}

		@Override
		public boolean isMasterAction() {
			return selector == null;
		}

		@Override
		public TargetSelector getTargetSelector() {
			return selector;
		}

		@Override
		public TargetAction getAction() {
			return action;
		}
	}
	
	private static class DeployExecutor implements TargetAction {

		private final BeanRef beanRef; 
		private final Deployable deployable;

		public DeployExecutor(BeanRef beanRef, Deployable deployable) {
			this.beanRef = beanRef;
			this.deployable = deployable;
		}


		@Override
		public Future<Void> submit(ViNode target, final Collection<ViNode> allTargets, TargetContext context) {
			DepolymentContext dc = new DepolymentContext() {
				@Override
				public Collection<ViNode> getDeploymentTargets() {
					return allTargets;
				}
			};

			DeploymentArtifact da = deployable.createArtifact(target, dc);
			return target.submit(new RemoteDeployAction(beanRef, context, da));
		}
		
		@Override
		public String toString() {
			return "DEPLOY " + beanRef + " " + deployable;
		}				
	}
	
	private static class RemoteDeployAction implements Callable<Void>, Serializable {
		
		private static final long serialVersionUID = 20121012L;
		
		private final BeanRef beanRef; 
		private final TargetContext context;
		private final DeploymentArtifact artifact;

		public RemoteDeployAction(BeanRef beanRef, TargetContext context, DeploymentArtifact artifact) {
			this.beanRef = beanRef;
			this.context = context;
			this.artifact = artifact;
		}

		@Override
		public Void call() throws Exception {
//			System.out.println("Context: " + context);
			Object bean = artifact.deploy(new EnvironmentContext() {				
			});
			context.deployBean(beanRef.id, bean);
			return null;
		}
		
		@Override
		public String toString() {
			return "DEPLOY " + beanRef + " " + artifact;
		}		
	}
	
	private static class CallExecutor implements TargetAction, Serializable {
		
		private static final long serialVersionUID = 20121016L;

		private final BeanRef beanRef;
		private final String methodClass;
		private final String methodToString;
		private final Object[] args;
		private final BeanRef deplotyTo;

		
		public CallExecutor(CallAction ca) {
			this(new BeanRef(ca.bean.id), ca.method, replaceWithBeanRef(ca.arguments), ca.deployTarget == null ? null : new BeanRef(ca.deployTarget.id));
		}

		private static Object[] replaceWithBeanRef(Object[] arguments) {
			Object[] args = Arrays.copyOf(arguments, arguments.length);
			for(int i = 0; i != args.length; ++i) {
				if (args[i] instanceof Bean) {
					BeanRef br = new BeanRef(((Bean)args[i]).id);
					args[i] = br;
				}
			}
			return args;
		}

		CallExecutor(BeanRef beanRef, Method method, Object[] args, BeanRef deplotyTo) {
			this.beanRef = beanRef;
			this.methodClass = method.getDeclaringClass().getName();
			this.methodToString = method.toString();
			this.args = args;
			this.deplotyTo = deplotyTo;
		}

		@Override
		public Future<Void> submit(ViNode node, Collection<ViNode> allTargets, TargetContext context) {
			return node.submit(new Call(context));
		}

		@Override
		public String toString() {
			String s1 = methodToString.substring(0, methodToString.indexOf('('));
			String mname = s1.substring(s1.lastIndexOf('.') + 1);
			return "CALL " + (deplotyTo == null ? "" : deplotyTo + " <- ") + beanRef + " " + methodClass.substring(methodClass.lastIndexOf('.') + 1) + "::" + mname;
		}		
		
		class Call implements Serializable, Callable<Void> {

			private static final long serialVersionUID = 20121016L;

			private final TargetContext context;
			
			private transient Method method;
			private transient Object bean;
			private transient Object result;

			public Call(TargetContext context) {
				this.context = context;
			}

			@Override
			public Void call() throws Exception {
//				System.out.println("Local context: " + context);
				bean = context.getBean(beanRef.id);
				if (bean == null) {
					throw new IllegalArgumentException("No bean found " + beanRef);
				}
				
				for(int i = 0; i != args.length; ++i) {
					if (args[i] instanceof BeanRef) {
						BeanRef br = (BeanRef)args[i];
						args[i] = context.getBean(br.id);
						if (args[i] == null) {
							throw new IllegalArgumentException("No bean found " + br);
						}
					}
				}
				
				method = resolveMethod();
				result = method.invoke(bean, args);
				
				if (deplotyTo != null) {
					context.deployBean(deplotyTo.id, result);
				}
				
				return null;
			}
			
			private Method resolveMethod() throws SecurityException, ClassNotFoundException, NoSuchMethodException {
				for(Method m: Class.forName(methodClass).getDeclaredMethods()) {
					if (m.toString().equals(methodToString)) {
						m.setAccessible(true);
						return m;
					}
				}
				throw new NoSuchMethodException(methodToString);
			}
			
			@Override
			public String toString() {
				return CallExecutor.this.toString();
			}		
		}
	}
		
	private static class CheckpointExecutor implements TargetAction {
		
		private final String name;
		private final long sleep;
		
		public CheckpointExecutor(String name, long sleepMs) {
			this.name = name;
			this.sleep = sleepMs;
		}

		@Override
		public Future<Void> submit(ViNode target, Collection<ViNode> allTargets, TargetContext context) {
			long nanodeadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(sleep) - 1;
			return new TimedFuture(nanodeadline);
		}
		
		@Override
		public String toString() {
			return "checkpoint " + name;
		}
	}
}

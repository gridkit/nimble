package org.gridkit.lab.jvm.attach;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

public class AttachManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttachManager.class);

    private static long ATTACH_TIMEOUT = TimeUnit.MILLISECONDS.toNanos(500);
    private static long VM_LIST_EXPIRY = TimeUnit.SECONDS.toNanos(1);
    private static long VM_PROPS_EXPIRY = TimeUnit.SECONDS.toNanos(1);
    private static long VM_MBEAN_SERVER_EXPIRY = TimeUnit.SECONDS.toNanos(30);

    static {
    	AttachAPI.ensureToolsJar();
    }
    
    private static AttachManagerInt INSTANCE = new AttachManagerInt();
    
    private static ExecutorService threadPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 500, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(), new ThreadFactory() {
    	int counter = 0;
    	
		@Override
		public synchronized Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setDaemon(true);
			t.setName("JvmAttachWorker-" + (counter++));
			return t;
		}
	}); 

    public static JavaProcessDetails getDetails(long pid) {
    	return INSTANCE.internalGetDetails(pid);
    }

    public static JavaProcessDetails getDetails(JavaProcessId jpid) {
    	return getDetails(jpid.getPID());
    }
    
    public static List<JavaProcessId> listJavaProcesses() {
    	return INSTANCE.internalListJavaProcesses();
    }

    public static List<JavaProcessId> listJavaProcesses(JavaProcessMatcher matcher) {
    	return INSTANCE.internalListJavaProcesses(matcher);
    }
    
    public static MBeanServerConnection getJmxConnection(long pid) {
    	return INSTANCE.internalGetJmxConnection(pid);
    }

    public static MBeanServerConnection getJmxConnection(JavaProcessId jpid) {
    	return getJmxConnection(jpid.getPID());
    }
    
    public static void loadAgent(long pid, String agentPath, String agentArgs, long timeoutMs) throws Exception {
        INSTANCE.internalLoadAgent(pid, agentPath, agentArgs, timeoutMs);
    }

    static class AttachManagerInt {    	
    	
	    private List<VirtualMachineDescriptor> vmList;
	    private long vmListEndOfLife;
	    
	    private Map<Long, Expirable<Properties>> vmPropsCache = new HashMap<Long, Expirable<Properties>>();            
	    private Map<Long, Expirable<MBeanServerConnection>> vmMBeanCache = new HashMap<Long, Expirable<MBeanServerConnection>>();
	    
	    private Map<Long, AttachRequest> attachQueue = new HashMap<Long, AttachRequest>();
	    
	    JavaProcessDetails internalGetDetails(long pid) {
	    	String name = getProcesssName(pid);
			return new ProcessDetails(pid, name);
		}
	
		private String getProcesssName(long pid) {
			List<VirtualMachineDescriptor> vms = getVmList();
			for(VirtualMachineDescriptor vm: vms) {
				if (vm.id().equals(String.valueOf(pid))) {
					return vm.displayName();
				}
			}
			return "";
		}
	
		private synchronized List<VirtualMachineDescriptor> getVmList() {
			if (vmList == null || vmListEndOfLife < System.nanoTime()) {
				vmList = VirtualMachine.list();
				vmListEndOfLife = System.nanoTime() + VM_LIST_EXPIRY;
			}
			return vmList;
		}
	
		List<JavaProcessId> internalListJavaProcesses() {
			List<VirtualMachineDescriptor> vms = getVmList();
			List<JavaProcessId> result = refine(vms);
			return result;
		}
	
		private List<JavaProcessId> refine(List<VirtualMachineDescriptor> vms) {
			JavaProcessId[] jpids = new JavaProcessId[vms.size()];
			for(int i = 0 ; i != jpids.length; ++i) {
				VirtualMachineDescriptor vm = vms.get(i);
				jpids[i] = new JavaProcessId(Long.parseLong(vm.id()), vm.displayName());
			}
			List<JavaProcessId> result = Arrays.asList(jpids);
			return result;
		}
	
		List<JavaProcessId> internalListJavaProcesses(final JavaProcessMatcher matcher) {
			
			List<Future<JavaProcessId>> futures = new ArrayList<Future<JavaProcessId>>();
			List<VirtualMachineDescriptor> vms = getVmList();
			for(final VirtualMachineDescriptor vm: vms) {
				futures.add(threadPool.submit(new Callable<JavaProcessId>() {
	
					@Override
					public JavaProcessId call() throws Exception {
						ProcessDetails pd = new ProcessDetails(Long.parseLong(vm.id()), vm.displayName());
						if (matcher.evaluate(pd)) {
							return new JavaProcessId(pd.pid, pd.name);
						}
						else {
							return null;
						}
					}
					
				}));			
			}
			
			List<JavaProcessId> result = new ArrayList<JavaProcessId>();
			for(Future<JavaProcessId> fp: futures) {
				try {
					JavaProcessId p = fp.get();
					if (p != null) {
						result.add(p);
					}
				}
				catch(InterruptedException e) {
					Thread.interrupted();
					return Collections.emptyList();
				}
				catch(ExecutionException e) {
					LOGGER.debug("Process filtering exception", e.getCause());
				}
			}
			
			return result;
		}
	
		synchronized MBeanServerConnection internalGetJmxConnection(long pid) {
			
			Expirable<MBeanServerConnection> mbh = vmMBeanCache.get(pid);
			if (mbh == null || mbh.expiryDeadline < System.nanoTime()) {
				MBeanServerConnection mserver = getMBeanServer(pid);
				mbh = new Expirable<MBeanServerConnection>(System.nanoTime() + VM_MBEAN_SERVER_EXPIRY, mserver);
				vmMBeanCache.put(pid, mbh);
			}
			
			return mbh.value;
		}
	
		Properties internalGetSystemProperties(long pid) {
			
			Expirable<Properties> proph;
			synchronized(this) {
				proph = vmPropsCache.get(pid);
			}
			if (proph == null || proph.expiryDeadline < System.nanoTime()) {
				Properties props = getSysProps(pid);
				proph = new Expirable<Properties>(System.nanoTime() + VM_PROPS_EXPIRY, props);
				synchronized(this) {
					vmPropsCache.put(pid, proph);
				}
			}
				
			return proph.value;
		}
		
		Properties internalGetAgentProperties(long pid) {
		    return getAgentProps(pid);
		}

        void internalLoadAgent(long pid, String agentPath, String agentArgs, long timeoutMs) throws Exception {
            try {
                attachAndPerform(pid, new LoadAgent(agentPath, agentArgs), TimeUnit.MILLISECONDS.toNanos(timeoutMs));
            }  catch (ExecutionException e) {
                if (e.getCause() instanceof AgentLoadException || e.getCause() instanceof AgentInitializationException) {
                    throw new Exception(e.getCause().getMessage());
                } else {
                    throw e;
                }
            } 
		}
	
		private MBeanServerConnection getMBeanServer(long pid) {
			try {
				String uri;
				try {
					uri = attachAndPerform(pid, new GetManagementAgent(), ATTACH_TIMEOUT);
				} catch (InterruptedException e) {
					LOGGER.warn("Cannot connect to JVM (" + pid + ") - interrupted");
					return null;
				} catch (ExecutionException e) {
					Throwable cause = e.getCause();
					if (cause instanceof AgentLoadException || cause instanceof AgentInitializationException) {
						LOGGER.debug("Cannot connect to JVM (" + pid + "). Agent error: " + e.toString());
						return null;
					}
					else {
						LOGGER.debug("Cannot connect to JVM (" + pid + ") - " + e.toString());
						return null;
					}
				} catch (TimeoutException e) {
					LOGGER.debug("Cannot connect to JVM (" + pid + ") - timeout");
					return null;
				}
	    		JMXServiceURL jmxurl = new JMXServiceURL(uri);
	    		JMXConnector conn = JMXConnectorFactory.connect(jmxurl);
	    		MBeanServerConnection mserver = conn.getMBeanServerConnection();
	    		return mserver;
				
			} catch (Exception e) {
				LOGGER.debug("Cannot connect to JVM (" + pid + ") - " + e.toString());
				return null;
			}
		}
	
		private Properties getSysProps(long pid) {
			try {
				return attachAndPerform(pid, new GetVmSysProps(), ATTACH_TIMEOUT);
			} catch (InterruptedException e) {
				LOGGER.warn("Failed to read system properties, JVM pid: " + pid + ", interrupted");
				return new Properties();
			} catch (ExecutionException e) {
				LOGGER.info("Failed to read system properties, JVM pid: " + pid + ", error: " + e.getCause().toString());
				return new Properties();
			} catch (TimeoutException e) {
				LOGGER.info("Failed to read system properties, JVM pid: " + pid + ", read timeout");
				return new Properties();
			}
		}

        private Properties getAgentProps(long pid) {
            try {
                return attachAndPerform(pid, new GetVmAgentProps(), ATTACH_TIMEOUT);
            } catch (InterruptedException e) {
                LOGGER.warn("Failed to read agent properties, JVM pid: " + pid + ", interrupted");
                return new Properties();
            } catch (ExecutionException e) {
                LOGGER.info("Failed to read agent properties, JVM pid: " + pid + ", error: " + e.getCause().toString());
                return new Properties();
            } catch (TimeoutException e) {
                LOGGER.info("Failed to read agent properties, JVM pid: " + pid + ", read timeout");
                return new Properties();
            }
        }
	
		@SuppressWarnings("unused")
		private static String attachManagementAgent(VirtualMachine vm) throws IOException, AgentLoadException, AgentInitializationException
		{
	     	Properties localProperties = vm.getAgentProperties();
	     	if (localProperties.containsKey("com.sun.management.jmxremote.localConnectorAddress")) {
	     		return ((String)localProperties.get("com.sun.management.jmxremote.localConnectorAddress"));
	     	}
			
			String jhome = vm.getSystemProperties().getProperty("java.home");
		    Object localObject = jhome + File.separator + "jre" + File.separator + "lib" + File.separator + "management-agent.jar";
		    File localFile = new File((String)localObject);
		    
		    if (!(localFile.exists())) {
		       localObject = jhome + File.separator + "lib" + File.separator + "management-agent.jar";
		 
		       localFile = new File((String)localObject);
		       if (!(localFile.exists())) {
		    	   throw new IOException("Management agent not found"); 
		       }
		    }
		 
	     	localObject = localFile.getCanonicalPath();     	
	 		vm.loadAgent((String)localObject, "com.sun.management.jmxremote");
	 
	     	localProperties = vm.getAgentProperties();
	     	return ((String)localProperties.get("com.sun.management.jmxremote.localConnectorAddress"));
	   	}
		
		private <V> V attachAndPerform(long pid, VMAction<V> action, long timeout) throws InterruptedException, ExecutionException, TimeoutException {
			VMTask<V> task = new VMTask<V>(action);
			boolean spawnThread = false;
			AttachRequest ar;
			synchronized(attachQueue) {
				ar = attachQueue.get(pid);
				if (ar != null) {
					ar.tasks.add(task);
				}
				else {
					ar = new AttachRequest(pid);
					ar.tasks.add(task);
					spawnThread = true;
					attachQueue.put(pid, ar);
				}				
			}
			if (spawnThread) {
				Thread attacher = new Thread(ar);
				attacher.setName("AttachToJVM-" + pid);
				attacher.setDaemon(true);
				attacher.start();				
			}

			try {
				V result = task.box.get(timeout, TimeUnit.MILLISECONDS);
				return result;
			}
			finally {
				task.box.cancel(false);
			}
		}
		
		@SuppressWarnings("unused")
		private static VirtualMachine attachToJvm(final String id)	throws AttachNotSupportedException, IOException {
			FutureTask<VirtualMachine> vmf = new FutureTask<VirtualMachine>(new Callable<VirtualMachine>() {
				@Override
				public VirtualMachine call() throws Exception {
					return VirtualMachine.attach(id);
				}
			});
			Thread attacher = new Thread(vmf);
			attacher.setName("AttachManager::attachToJvm(" + id + ")");
			attacher.setDaemon(true);
			attacher.start();
			try {
				return vmf.get(ATTACH_TIMEOUT, TimeUnit.NANOSECONDS);
			} catch (Exception e) {
				IOException er;
				if (e instanceof ExecutionException) {
					er = new IOException(e.getCause());
				}
				else {
					er = new IOException(e);
				}
				if (attacher.isAlive()) {
					attacher.interrupt();
				}
				throw er;
			}
		}    

		@SuppressWarnings("unused")
		private static Properties getSysPropsAsync(final String id) {
			FutureTask<Properties> vmf = new FutureTask<Properties>(new Callable<Properties>() {
				@Override
				public Properties call() throws Exception {
					VirtualMachine vm = null;
					try {
						vm = VirtualMachine.attach(id);
						return vm.getSystemProperties();
					} finally {
						if (vm != null) {
							vm.detach();
						}
					}
				}
			});
			Thread attacher = new Thread(vmf);
			attacher.setName("AttachManager::getSysPropsAsync(" + id + ")");
			attacher.setDaemon(true);
			attacher.start();
			try {
				return vmf.get(ATTACH_TIMEOUT, TimeUnit.NANOSECONDS);
			} catch (Exception e) {
				Throwable x = e;
				if (e instanceof ExecutionException) {
					x = e.getCause();
				}
				if (attacher.isAlive()) {
					attacher.interrupt();
				}
				LOGGER.info("Attach to (" + id + ") has failed: " + x.toString());
				LOGGER.debug("Attach to (" + id + ") has failed", x);

				return new Properties();
			}
		}    
		
		private class ProcessDetails implements JavaProcessDetails {
			
			private final long pid;
			private final String name;
			
			public ProcessDetails(long pid, String name) {
				this.pid = pid;
				this.name = name;
			}
	
			@Override
			public long getPid() {
				return pid;
			}
			
			@Override
			public String getDescription() {
				return name;
			}
			
			@Override
			public JavaProcessId getJavaProcId() {
				return new JavaProcessId(pid, name);
			}
	
			@Override
			public Properties getSystemProperties() {
				return internalGetSystemProperties(pid);
			}
			
            @Override
            public Properties getAgentProperties() {
                return internalGetAgentProperties(pid);
            }
			
			@Override
			public MBeanServerConnection getMBeans() {
				return internalGetJmxConnection(pid);
			}
		}
		
		private static class Expirable<V> {
	    	
	    	long expiryDeadline;
	    	V value;
			
	    	public Expirable(long expiryDeadline, V value) {
				this.expiryDeadline = expiryDeadline;
				this.value = value;
			}
	    }
		
		private class AttachRequest implements Runnable {
			
			final long pid;			
			final List<VMTask<?>> tasks;
			
			public AttachRequest(long pid) {
				this.pid = pid;
				this.tasks = new ArrayList<VMTask<?>>();
			}

			@Override
			public void run() {
				VirtualMachine vm;
				try {
					try {
						vm = VirtualMachine.attach(String.valueOf(pid));
					}
					catch(IOException e) {
						// second try
						vm = VirtualMachine.attach(String.valueOf(pid));
					}
					dispatch(vm);
				} catch (Throwable e) {
					fail(e);
					return;
				}
				try {
					vm.detach();
				} catch (IOException e) {
					// ignore
				}
			}

			private void dispatch(VirtualMachine vm) {
				while(true) {
					VMTask<?> task;
					synchronized(attachQueue) {
						if (tasks.isEmpty()) {
							attachQueue.remove(pid);
							return;
						}
						else {
							task = tasks.remove(0);
						}
					}
					task.perform(vm);
				}
			}

			private void fail(Throwable e) {
				LOGGER.info("Attach to (" + pid + ") has failed: " + e.toString());
				LOGGER.debug("Attach to (" + pid + ") has failed", e);
				while(true) {
					VMTask<?> task;
					synchronized(attachQueue) {
						if (tasks.isEmpty()) {
							attachQueue.remove(pid);
							return;
						}
						else {
							task = tasks.remove(0);
						}
					}
					task.fail(e);
				}
			}
		}
		
		private static class FutureBox<V> extends FutureTask<V> {

			private FutureBox(Callable<V> callable) {
				super(callable);
			}

			@Override
			public void setException(Throwable t) {
				super.setException(t);
			}
		}
		
		private static class VMTask<V> {
			
			VirtualMachine vm;
			VMAction<V> action;
			FutureBox<V> box;
			
			public VMTask(VMAction<V> action) {
				this.action = action;
				this.box = new FutureBox<V>(new Callable<V>() {
					@Override
					public V call() throws Exception {
						return VMTask.this.action.perform(vm);
					}
				});
			}
			
			public void perform(VirtualMachine vm) {
				this.vm = vm;
				box.run();
				this.vm = null;
			}			

			public void fail(Throwable e) {
				box.setException(e);
			}			
		}
		
		private static interface VMAction<V> {

			public V perform(VirtualMachine vm) throws Exception;
		}
		
		private static class GetVmSysProps implements VMAction<Properties> {

			@Override
			public Properties perform(VirtualMachine vm) throws IOException {
				return vm.getSystemProperties();
			}
		}
		
        private static class GetVmAgentProps implements VMAction<Properties> {

            @Override
            public Properties perform(VirtualMachine vm) throws IOException {
                return vm.getAgentProperties();
            }
        }

		private static class GetManagementAgent implements VMAction<String> {
			
			@Override
			public String perform(VirtualMachine vm) throws IOException, AgentLoadException, AgentInitializationException {
		     	Properties localProperties = vm.getAgentProperties();
		     	if (localProperties.containsKey("com.sun.management.jmxremote.localConnectorAddress")) {
		     		return ((String)localProperties.get("com.sun.management.jmxremote.localConnectorAddress"));
		     	}
				
				String jhome = vm.getSystemProperties().getProperty("java.home");
			    Object localObject = jhome + File.separator + "jre" + File.separator + "lib" + File.separator + "management-agent.jar";
			    File localFile = new File((String)localObject);
			    
			    if (!(localFile.exists())) {
			       localObject = jhome + File.separator + "lib" + File.separator + "management-agent.jar";
			 
			       localFile = new File((String)localObject);
			       if (!(localFile.exists())) {
			    	   throw new IOException("Management agent not found"); 
			       }
			    }
			 
		     	localObject = localFile.getCanonicalPath();     	
		 		vm.loadAgent((String)localObject, "com.sun.management.jmxremote");
		 
		     	localProperties = vm.getAgentProperties();
		     	return ((String)localProperties.get("com.sun.management.jmxremote.localConnectorAddress"));
			}
		}
		
		private static class LoadAgent implements VMAction<Void> {
		    private final String agentPath;
		    private final String agentArgs; 

            public LoadAgent(String agentPath, String agentArgs) {
                this.agentPath = agentPath;
                this.agentArgs = agentArgs;
            }

            @Override
            public Void perform(VirtualMachine vm) throws Exception {
                vm.loadAgent(agentPath, agentArgs);
                return null;
            }
		}
    }
}

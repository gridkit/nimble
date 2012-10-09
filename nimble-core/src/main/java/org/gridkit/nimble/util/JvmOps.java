package org.gridkit.nimble.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.gridkit.nimble.sensor.JvmMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

@SuppressWarnings("restriction")
public class JvmOps {
    private static final Logger log = LoggerFactory.getLogger(JvmOps.class);
    
    static {
        try {
            String javaHome = System.getProperty("java.home");
            String toolsJarURL = "file:" + javaHome + "/../lib/tools.jar";

            // Make addURL public
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            
            URLClassLoader sysloader = (URLClassLoader)ClassLoader.getSystemClassLoader();
            method.invoke(sysloader, (Object) new URL(toolsJarURL));
        } catch (Exception e) {
            log.error("Failed to add tools.jar to classpath", e);
        }
    }

    private static List<VirtualMachineDescriptor> vmList;
    private static long vmListTimestamp;
    
    private static LoadingCache<VirtualMachineDescriptor, Properties> vmPropsCache =
            CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build(new JvmPropsLoader());

    private static LoadingCache<VirtualMachineDescriptor, MBeanServerConnection> vmMBeanCache =
    		CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build(new MBeanLoader());
    
    private static class JvmPropsLoader extends CacheLoader<VirtualMachineDescriptor, Properties> {
        @Override
        public Properties load(VirtualMachineDescriptor key) throws Exception {
            VirtualMachine vm = null;
        
            try {
            	System.out.println("Loading system properties for " + key.id());
                vm = attach(key);                
                return vm.getSystemProperties();
            } catch (Exception e) {
                log.warn("Failed to retrieve JVM properties of " + key + ". Exception: " + e.toString());
                return new Properties();
            } finally {
                if (vm != null) {
                    try {
                    	dettach(vm);
                    } catch (IOException e) {
                        log.warn("Failed to detach from vm " + key, e);
                    }
                }
            }
        }
    }

    private static class MBeanLoader extends CacheLoader<VirtualMachineDescriptor, MBeanServerConnection> {

		@Override
		public MBeanServerConnection load(VirtualMachineDescriptor key) throws Exception {
            VirtualMachine vm = null;
            
            try {
        		vm = attach(key);
        		String addr = attachManagementAgent(vm);
        		System.out.println("JVM JMX uri: " + addr);			
        		JMXServiceURL jmxurl = new JMXServiceURL(addr);
        		JMXConnector conn = JMXConnectorFactory.connect(jmxurl);
        		MBeanServerConnection mserver = conn.getMBeanServerConnection();
        		System.out.println("MBean server connected");                
                return mserver;
            } finally {
                if (vm != null) {
                    try {
                        dettach(vm);
                    } catch (IOException e) {
                        log.warn("Failed to detach from vm " + key, e);
                    }
                }
            }
		}
    }
    
    public static synchronized List<VirtualMachineDescriptor> listVms() {
    	if (vmList == null || vmListTimestamp + TimeUnit.SECONDS.toNanos(5) < System.nanoTime()) {
    		System.out.println("Listing JVM processes ...");
    		vmList = VirtualMachine.list();
    		vmListTimestamp = System.nanoTime();
    	}
        return new ArrayList<VirtualMachineDescriptor>(vmList);
    }

    public static List<VirtualMachineDescriptor> listVms(final JvmMatcher filter) {
    	List<VirtualMachineDescriptor> vms = listVms();
    	if (vms.isEmpty()) {
    		return vms;
    	}
    	final List<VirtualMachineDescriptor> result = Collections.synchronizedList(new ArrayList<VirtualMachineDescriptor>());
    	ExecutorService es = Executors.newFixedThreadPool(vms.size());
    	for(final VirtualMachineDescriptor vm: vms) {
    		es.submit(new Runnable() {
				@Override
				public void run() {
					if (filter.matches(vm)) {
						result.add(vm);
					}					
				}
			});
    	}
    	es.shutdown();
    	try {
			es.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// ignore
		}
    	
        return result;
    }
    
    
    public static Properties getProps(VirtualMachineDescriptor desc) {
        try {
            return vmPropsCache.get(desc);
        } catch (ExecutionException e) {
            log.warn("Failed to retrieve JVM properties of " + desc + ". Exception: " + e.getCause().toString());
            return null;
        }
    }
    
    public static MBeanServerConnection getMBeanConnection(VirtualMachineDescriptor desc) {
    	try {
    		return vmMBeanCache.get(desc);
        } catch (ExecutionException e) {
            log.warn("Failed to retrieve MBeanServer connection for " + desc + ". Exception: " + e.getCause().toString());
            return null;
        }
    }
    
	private static String attachManagementAgent(VirtualMachine vm) throws Exception
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
     	try {
     		vm.loadAgent((String)localObject, "com.sun.management.jmxremote");
     	} catch (Exception e) {
     		throw e;
     	}
 
     	localProperties = vm.getAgentProperties();
     	return ((String)localProperties.get("com.sun.management.jmxremote.localConnectorAddress"));
   	}
	
	private static VirtualMachine attach(final VirtualMachineDescriptor vmd)	throws AttachNotSupportedException, IOException {
//		System.out.println("Attaching: " + vmd.id() + "/" + vmd.displayName());
		FutureTask<VirtualMachine> vmf = new FutureTask<VirtualMachine>(new Callable<VirtualMachine>() {
			@Override
			public VirtualMachine call() throws Exception {
				return VirtualMachine.attach(vmd);
			}
		});
		Thread attacher = new Thread(vmf);
		attacher.setDaemon(true);
		attacher.start();
		try {
			return vmf.get(1, TimeUnit.SECONDS);
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

	private static void dettach(VirtualMachine vm)	throws AttachNotSupportedException, IOException {
		vm.detach();
	}    
}

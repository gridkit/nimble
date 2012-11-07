package org.gridkit.nimble.btrace;

import java.io.Serializable;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import net.java.btrace.client.Client;
import net.java.btrace.ext.Printer;

import org.gridkit.nimble.btrace.ext.Nimble;
import org.gridkit.nimble.util.SystemOps;
import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.VoidCallable;
import org.gridkit.vicluster.telecontrol.LocalJvmProcessFactory;
import org.gridkit.vicluster.telecontrol.jvm.JvmNodeProvider;
import org.gridkit.vicluster.telecontrol.jvm.JvmProps;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class BTraceClientFactoryTest {    
    private static ViManager cloud;
    
    private static AtomicInteger counter = new AtomicInteger(0);
    
    private static BTraceClientOps clientOps = new BTraceClientOps();
    
    private static long OP_TIMEOUT_MS = 1000;
    
    @BeforeClass
    public static void beforeClass() {
        cloud = new ViManager(new JvmNodeProvider(new LocalJvmProcessFactory()));
        JvmProps.addJvmArg(cloud.node("**"), "-XX:MaxPermSize=512m");
        JvmProps.addJvmArg(cloud.node("**"), "-Xmx512m");
        JvmProps.addJvmArg(cloud.node("**"), "-XX:MaxDirectMemorySize=512m");
    }

    @AfterClass
    public static void afterClass() {
        try {
            cloud.shutdown();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    @After
    public void after() throws Exception {
        Thread.sleep(2000); // release TCP ports
    }

    private void simple_connect(Class<?> clazz) throws Exception {        
        BTraceClientFactory factory = new BTraceClientFactory();
        
        ViNode node = newNode("connect");
        
        try {
            int pid = node.exec(new GetPid());
            
            BTraceClientSettings settings = newSettings();
            
            Client client = factory.newClient(pid, settings);
            
            submit(client, clazz);
    
            Assert.assertTrue(ping(client));
            
            Thread.sleep(1000);
                                
            exit(client);
        } finally {
            node.shutdown();
        }
    }

    private static boolean ping(Client client) throws Exception {
        return clientOps.clearSamples(client, Collections.<Class<?>>emptySet(), OP_TIMEOUT_MS);
    }
    
    @Test
    public void connect() throws Exception {
        simple_connect(ThreadCountScript.class);
    }
    
    @Test(expected = TimeoutException.class)
    public void connect_incorrect_script() throws Exception {
        simple_connect(IncorrectScript.class);
    }
    
    @Test
    public void busy_port_connect() throws Exception {
        ServerSocket ss = null;

        ss = new ServerSocket(BTraceClientFactory.BTRACE_PORT);
        ss.setReuseAddress(true);

        simple_connect(ThreadCountScript.class);
        
        ss.close();
    }

    @Test
    public void two_clients() throws Exception {
        BTraceClientFactory factory = new BTraceClientFactory();
        
        ViNode node = newNode("node");
        
        try {
            int pid = node.exec(new GetPid());
            
            BTraceClientSettings settings = newSettings();
    
            Client client1 = factory.newClient(pid, settings);
            Client client2 = factory.newClient(pid, settings);
            
            submit(client1, ThreadCountScript.class);
            submit(client2, ThreadCountScript.class);
    
            Assert.assertTrue(ping(client1));
            Assert.assertTrue(ping(client2));
            
            exit(client1);
            exit(client2);
        } finally {
            node.shutdown();
        }
    }
    
    private ViNode newNode(String name) {
        return cloud.node(name + "-" + counter.getAndIncrement());
    }
    
    private BTraceClientSettings newSettings() {
        BTraceClientSettings settings = new BTraceClientSettings();
        
        settings.setExtensionClasses(Nimble.class, Printer.class);
        
        return settings;
    }
    
    private static void exit(Client client) throws ExecutionException, InterruptedException {
        try {
            clientOps.exit(client, 0, OP_TIMEOUT_MS);
        } catch (TimeoutException ignored) {
        }
    }
    
    private static void submit(Client client, Class<?> clazz) throws TimeoutException, ExecutionException, InterruptedException  {
        clientOps.submit(client, clazz, new String[] {}, OP_TIMEOUT_MS);
    }

    @SuppressWarnings("serial")
    public static class GetPid implements Callable<Integer>, Serializable {
        @Override
        public Integer call() throws Exception {
            return SystemOps.getPid();
        }
    }
    
    @SuppressWarnings("serial")
    public static class Exit implements VoidCallable, Serializable {
        @Override
        public void call() throws Exception {
            Thread.sleep(1000);
            
            System.exit(1);
        }
    };
}

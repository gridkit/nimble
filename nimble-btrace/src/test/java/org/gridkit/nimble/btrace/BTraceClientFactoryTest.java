package org.gridkit.nimble.btrace;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import net.java.btrace.client.Client;
import net.java.btrace.ext.Printer;

import org.gridkit.nimble.btrace.ext.PingCmd;
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
    
    @BeforeClass
    public static void beforeClass() {
        cloud = new ViManager(new JvmNodeProvider(new LocalJvmProcessFactory()));
        JvmProps.setJvmArg(cloud.node("**"), "-XX:MaxPermSize=512m");
        JvmProps.setJvmArg(cloud.node("**"), "-Xmx512m");
        JvmProps.setJvmArg(cloud.node("**"), "-XX:MaxDirectMemorySize=512m");
    }

    @After
    public void after() throws Exception {
        Thread.sleep(1000); // release TCP ports
    }
    
    @Test
    public void connect() throws Exception {
        BTraceClientFactory factory = new BTraceClientFactory();
        
        ViNode node = newNode("connect");
        
        int pid = node.exec(getPid);
        
        BTraceClientSettings settings = newSettings();
        
        Client client = factory.newClient(pid, settings);
        
        submit(client);

        Assert.assertTrue(ping(client));
        
        Thread.sleep(1000); // wait for print
        
        factory.exit(client, 0);

        node.shutdown();
    }
    
    @Test
    public void busy_port_connect() throws Exception {
        ServerSocket ss = null;

        ss = new ServerSocket(BTraceClientFactory.BTRACE_PORT);
        ss.setReuseAddress(true);

        connect();
        
        ss.close();
    }

    @Test
    public void two_clients() throws Exception {
        BTraceClientFactory factory = new BTraceClientFactory();
        
        ViNode node = newNode("node");
        int pid = node.exec(getPid);
        
        BTraceClientSettings settings = newSettings();

        Client client1 = factory.newClient(pid, settings);
        Client client2 = factory.newClient(pid, settings);
        
        submit(client1);
        submit(client2);

        Assert.assertTrue(ping(client1));
        Assert.assertTrue(ping(client2));
        
        factory.exit(client1, 0);
        factory.exit(client2, 0);
        
        node.shutdown();
    }
    
    private ViNode newNode(String name) {
        return cloud.node(name + "-" + counter.getAndIncrement());
    }
    
    private BTraceClientSettings newSettings() {
        BTraceClientSettings settings = new BTraceClientSettings();
        
        settings.setExtensionClasses(Printer.class);
        
        return settings;
    }
    
    private static void submit(Client client) throws IOException {
        client.submit(BTraceClientFactoryTestScript.class.getName(), getBytes(BTraceClientFactoryTestScript.class), new String[] {});
    }
    
    private static Boolean ping(Client client) throws InterruptedException, IOException {
        return (Boolean)client.getCommChannel().sendCommand(PingCmd.class).get();
    }
    
    @AfterClass
    public static void afterClass() {
        try {
            cloud.shutdown();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    public static Callable<Integer> getPid = new Callable<Integer>() {
        @Override
        public Integer call() throws Exception {
            return SystemOps.getPid();
        }
    };
    
    public static VoidCallable exit = new VoidCallable() {
        @Override
        public void call() throws Exception {
            Thread.sleep(1000);
            
            System.exit(1);
        }
    };
    
    private static byte[] getBytes(Class<?> clazz) throws IOException {
        InputStream is = clazz.getClassLoader().getResourceAsStream(clazz.getName().replace('.', '/') + ".class");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
          buffer.write(data, 0, nRead);
        }

        buffer.flush();

        byte[] result = buffer.toByteArray();
        
        return result;
    }
}

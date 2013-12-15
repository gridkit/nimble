package org.gridkit.nimble.btrace;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nimble.btrace.ext.PollSamplesCmdResult;
import org.gridkit.nimble.btrace.ext.model.ScalarSample;
import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViProps;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class BTraceClientFactoryTest {    
    private static ViManager cloud;
    
    private static AtomicInteger counter = new AtomicInteger(0);
        
    private static long OP_TIMEOUT_MS = 3000;
    
    @BeforeClass
    public static void beforeClass() {
        cloud = CloudFactory.createCloud();
        ViProps.at(cloud.node("**")).setLocalType();
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
        Thread.sleep(5000); // release TCP ports
    }

    @Test
    public void one_client() throws Exception {
        int from1 = 0;
        int from2 = from1 + CountScript.STORE_SIZE;
        int from3 = from2 + CountScript.STORE_SIZE;
        
        int nTicks1 = CountScript.STORE_SIZE / 2;
        int nTicks2 = 0;
        int nTicks3 = CountScript.STORE_SIZE + 1;
        
        BTraceClientFactory factory = new BTraceClientFactory(newClientSettings());
        
        ViNode node = newNode("one_client");
        
        try {
            int pid = node.exec(new GetPid());
                        
            NimbleClient client = factory.newClient(pid, newScriptSettings(CountScript.class));
            
            Assert.assertTrue(client.submit());
            Assert.assertTrue(client.configureSession());

            node.exec(Count.newCounter(from1, nTicks1));
            PollSamplesCmdResult result1 = client.pollSamples();
            validate(result1, from1, nTicks1);
            
            PollSamplesCmdResult result2 = client.pollSamples();
            validate(result2, from2, nTicks2);
            
            node.exec(Count.newCounter(from3, nTicks3));
            PollSamplesCmdResult result3 = client.pollSamples();
            validate(result3, from3 + 1, nTicks3 - 1);

            client.close();
        } finally {
            node.shutdown();
        }
    }
    
    @Test
    public void two_clients() throws Exception {
        int from0 = 0;
        int from1 = from0 + CountScript.STORE_SIZE;
        int from2 = from1 + CountScript.STORE_SIZE;
        int from3 = from2 + CountScript.STORE_SIZE;
        int from4 = from3 + CountScript.STORE_SIZE;
        
        int nTicks0 = CountScript.STORE_SIZE / 2;
        int nTicks1 = CountScript.STORE_SIZE / 3;
        int nTicks2 = 0;
        int nTicks3 = CountScript.STORE_SIZE + 1;
        int nTicks4 = CountScript.STORE_SIZE / 4;
        
        BTraceClientFactory factory = new BTraceClientFactory(newClientSettings());
        
        ViNode node = newNode("two_clients");
        
        try {
            int pid = node.exec(new GetPid());
                
            NimbleClient client1 = factory.newClient(pid, newScriptSettings(CountScript.class));
            NimbleClient client2 = factory.newClient(pid, newScriptSettings(CountScript.class));
            
            Assert.assertTrue(client1.submit());
            Assert.assertTrue(client1.configureSession());
            
            node.exec(Count.newCounter(from0, nTicks0));
            PollSamplesCmdResult result0 = client1.pollSamples();
            validate(result0, from0, nTicks0);
            
            Assert.assertTrue(client2.submit());
            Assert.assertTrue(client2.configureSession());
            
            node.exec(Count.newCounter(from1, nTicks1));
            PollSamplesCmdResult result11 = client1.pollSamples();
            PollSamplesCmdResult result12 = client2.pollSamples();
            validate(result11, from1, nTicks1);
            validate(result12, from1, nTicks1);
            
            PollSamplesCmdResult result21 = client1.pollSamples();
            PollSamplesCmdResult result22 = client2.pollSamples();
            validate(result21, from2, nTicks2);
            validate(result22, from2, nTicks2);
            
            node.exec(Count.newCounter(from3, nTicks3));
            PollSamplesCmdResult result31 = client1.pollSamples();
            PollSamplesCmdResult result32 = client2.pollSamples();
            validate(result31, from3 + 1, nTicks3 - 1);
            validate(result32, from3 + 1, nTicks3 - 1);

            client1.close();
            
            node.exec(Count.newCounter(from4, nTicks4));
            PollSamplesCmdResult result42 = client2.pollSamples();
            validate(result42, from4, nTicks4);
            
            client2.close();
        } finally {
            node.shutdown();
        }
    }

    private static void validate(PollSamplesCmdResult result, int from, int nTicks) {
        Assert.assertEquals(1, result.getData().size());
        
        List<ScalarSample> samples = result.getData().get(0).getSamples();
        
        Assert.assertEquals(nTicks, samples.size());
        
        for (int i = 0; i < nTicks; ++i) {
            Assert.assertEquals(from + i, samples.get(i).getValue());
        }
    }
    
    @Test
    public void incorrect_script_false() throws Exception {
        BTraceClientFactory factory = new BTraceClientFactory(newClientSettings());
        
        ViNode node = newNode("incorrect_script");
        
        NimbleClient client = null;
        
        try {
            int pid = node.exec(new GetPid());
                        
            client = factory.newClient(pid, newScriptSettings(IncorrectScript.class));

            Assert.assertFalse(client.submit());

            Thread.sleep(1000);
            
            try {
                client.configureSession();
            } catch (Exception e) {
                Assert.assertEquals(IllegalStateException.class, e.getClass());
            }
            
            client.close();
        } finally {
            node.shutdown();
        }
    }
    
    @Test
    public void incorrect_script_true() throws Exception {
        BTraceClientSettings settings = newClientSettings();
        settings.setUnsafe(true);
        BTraceClientFactory factory = new BTraceClientFactory(settings);
        
        ViNode node = newNode("incorrect_script");
        
        NimbleClient client = null;
        
        try {
            int pid = node.exec(new GetPid());
                        
            client = factory.newClient(pid, newScriptSettings(IncorrectScript.class));

            Assert.assertTrue(client.submit());
            Assert.assertTrue(client.configureSession());
            
            client.close();
        } finally {
            node.shutdown();
        }
    }
    
    @Test
    public void busy_port_connect() throws Exception {
        ServerSocket ss = null;

        ss = new ServerSocket(BTraceClientFactory.getNextPort());
        ss.setReuseAddress(true);

        one_client();
        
        ss.close();
    }
    
    private ViNode newNode(String name) {
        ViNode node = cloud.node(name + "-" + counter.getAndIncrement());
        
        node.exec(Count.newCounter(1, 1));
        
        return node;
    }
    
    private BTraceClientSettings newClientSettings() {
        BTraceClientSettings settings = new BTraceClientSettings();
        
        if (BTTestHelper.isNimbleExpanded()) {
        	settings.addExtension(BTTestHelper.getNimbleBTraceJar());
        }
        
        settings.setDebug(true);
        
        return settings;
    }
    
    private BTraceScriptSettings newScriptSettings(Class<?> clazz) {
        BTraceScriptSettings settings = new BTraceScriptSettings();
        
        settings.setScriptClass(clazz);
        settings.setTimeoutMs(OP_TIMEOUT_MS);
        
        return settings;
    }
    
    @SuppressWarnings("serial")
    public static class GetPid implements Callable<Integer>, Serializable {
        @Override
        public Integer call() throws Exception {
            return getPid();
        }
    }
    
    public static int getPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        
        int dogIndex = name.indexOf('@');
        
        if (dogIndex != -1) {
            try {
                return Integer.valueOf(name.substring(0, dogIndex));
            } catch (NumberFormatException e) {
                return -1;
            }
        } else {
            return -1;
        }
    }
}

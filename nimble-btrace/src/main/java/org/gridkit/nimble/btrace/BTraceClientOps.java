package org.gridkit.nimble.btrace;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.java.btrace.client.Client;

import org.gridkit.nimble.btrace.ext.PingCmd;
import org.gridkit.nimble.btrace.ext.PollSamplesCmd;
import org.gridkit.nimble.btrace.ext.PollSamplesCmdResult;
import org.gridkit.nimble.util.NamedThreadFactory;

public class BTraceClientOps {    
    private static long INTERRUPT_DELAY_MS = 1000;
    
    private ExecutorService executor = Executors.newCachedThreadPool(
        new NamedThreadFactory(this.getClass() + "Executor", true, Thread.NORM_PRIORITY)
    );
        
    private ScheduledExecutorService interruptExecutor = Executors.newSingleThreadScheduledExecutor(
        new NamedThreadFactory(this.getClass() + "InterruptExecutor", true, Thread.MIN_PRIORITY)
    );
    
    public boolean ping(Client client, long timeoutMs) throws InterruptedException, IOException {
        return (Boolean) client.getCommChannel().sendCommand(PingCmd.class).get(timeoutMs);
    }
    
    public void submit(final Client client, final Class<?> clazz, final String[] params, long timeoutMs) throws TimeoutException, ExecutionException, InterruptedException {
        execute(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                client.submit(clazz.getName(), getClassBytes(clazz), params);
                return null;
            }
        }, timeoutMs);

    }
    
    public Map<String, PollSamplesCmdResult<?>> poll(Client client, long delayMs) throws InterruptedException, IOException {
        @SuppressWarnings("unchecked")
        Map<String, PollSamplesCmdResult<?>> result = 
            (Map<String, PollSamplesCmdResult<?>>)client.getCommChannel().sendCommand(PollSamplesCmd.class).get(delayMs);

        return result;
    }
    
    public void exit(final Client client, final int exitCode, long timeoutMs) throws TimeoutException, ExecutionException, InterruptedException {
        execute(new Callable<Void>() {
            @Override
            public Void call() {
                client.exit(exitCode);
                return null;
            }
        }, timeoutMs);
    }
    
    private <T> T execute(Callable<T> callable, long timeoutMs) throws TimeoutException, ExecutionException, InterruptedException {
        final Future<T> exitFuture = executor.submit(callable);
        
        try {
            return exitFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
        } finally {
            interruptExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    exitFuture.cancel(true);
                }
            }, INTERRUPT_DELAY_MS, TimeUnit.MILLISECONDS);
        }
    }
    
    private static byte[] getClassBytes(Class<?> clazz) throws IOException {
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

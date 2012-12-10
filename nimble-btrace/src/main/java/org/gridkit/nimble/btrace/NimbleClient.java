package org.gridkit.nimble.btrace;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.java.btrace.api.extensions.ExtensionsRepository;
import net.java.btrace.api.wireio.AbstractCommand;
import net.java.btrace.api.wireio.Channel;
import net.java.btrace.client.Client;
import net.java.btrace.instr.ClassRenamer;
import net.java.btrace.instr.InstrumentUtils;
import net.java.btrace.org.objectweb.asm.ClassReader;
import net.java.btrace.org.objectweb.asm.ClassWriter;
import net.java.btrace.wireio.commands.ExitCommand;

import org.gridkit.nimble.btrace.ext.ConfigureSessionCmd;
import org.gridkit.nimble.btrace.ext.PollSamplesCmd;
import org.gridkit.nimble.btrace.ext.PollSamplesCmdResult;
import org.gridkit.nimble.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NimbleClient extends Client {
    private static final Logger log = LoggerFactory.getLogger(NimbleClient.class);
    
    private static final Random RND = new Random();
    
    private static final long INTERRUPT_DELAY_MS = 1000;
    
    protected static final ExecutorService executor = Executors.newCachedThreadPool(
        new NamedThreadFactory("NimbleBTraceClientExecutor", true, Thread.NORM_PRIORITY)
    );
            
    protected static final ScheduledExecutorService interruptExecutor = Executors.newSingleThreadScheduledExecutor(
        new NamedThreadFactory("NimbleBTraceClientInterruptExecutor", true, Thread.MIN_PRIORITY)
    );
    
    private final BTraceScriptSettings settings;
    
    private Socket socket;
    
    private String traceSriptClass;
    
    protected NimbleClient(int pid, BTraceScriptSettings settings) throws Exception {
        super(pid);
        this.settings = settings;
    }
    
    /**
     *  should be called first because it is the only method initializing command channel
     */
    public synchronized boolean submit() throws Exception {
        execute(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                submit(settings.getScriptClass().getName(), getBTraceScriptBytes(settings.getScriptClass()), settings.getArgsArray());
                return null;
            }
        });
        
        return isRunning();
    }
    
    public synchronized boolean configureSession() throws Exception {
        ensureRunning();
        
        traceSriptClass = (String)this.getCommChannel()
                                      .sendCommand(ConfigureSessionCmd.class)
                                      .get(settings.getTimeoutMs());
        
        return traceSriptClass != null;
    }
    
    public synchronized PollSamplesCmdResult pollSamples() throws Exception {
        ensureRunning();
        
        if (traceSriptClass == null) {
            throw new IllegalStateException("Session is not configured");
        }
        
        AbstractCommand.Initializer<PollSamplesCmd> initializer = new AbstractCommand.Initializer<PollSamplesCmd>() {
            @Override
            public void init(PollSamplesCmd cmd) {                
                cmd.setTraceSriptClass(traceSriptClass);
            }
        };
        
        PollSamplesCmdResult result = (PollSamplesCmdResult)this.getCommChannel()
                                                                .sendCommand(PollSamplesCmd.class, initializer)
                                                                .get(settings.getTimeoutMs());
        return result;
    }
        
    public synchronized void close() {
        try {
            try {
                super.exit(0);
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Exception while closing BTrace client", e);
        }
    }
    
    @Override
    protected void sendExit(final int exitCode) {
        try {
            getCommChannel().sendCommand(ExitCommand.class, new AbstractCommand.Initializer<ExitCommand>() {
                public void init(ExitCommand cmd) {
                    cmd.setExitCode(exitCode);
                }
            });
        } catch (IOException e) {
            log.warn("Error sending exit to BTrace server", e);
        }
    }
    
    @Override
    protected Channel newClientChannel(Socket sock, ExtensionsRepository extRepository) {
        this.socket = sock;
        return NimbleClientChannel.open(sock, extRepository);
    }
    
    @Override
    public void agentExit(int exitCode) {        
        setState(State.OFFLINE);
    }
    
    private boolean isRunning() {
        return state.get() == State.RUNNING;
    }
    
    private void ensureRunning() {
        if (!isRunning()) {
            throw new IllegalStateException("BTrace client is not running");
        }
    }
    
    private <T> T execute(Callable<T> callable) throws TimeoutException, ExecutionException, InterruptedException {
        return execute(callable, settings.getTimeoutMs());
    }
    
    protected static <T> T execute(Callable<T> callable, long timeoutMs) throws TimeoutException, ExecutionException, InterruptedException {
        final Future<T> future = executor.submit(callable);
        
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } finally {
            interruptExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    future.cancel(true);
                }
            }, INTERRUPT_DELAY_MS, TimeUnit.MILLISECONDS);
        }
    }
    
    private static byte[] getBTraceScriptBytes(Class<?> clazz) throws IOException {
        InputStream is = null;
        
        try {
            String classResource = clazz.getName().replace('.', '/') + ".class";
            
            is = new BufferedInputStream(clazz.getClassLoader().getResourceAsStream(classResource));
    
            ClassReader reader = new ClassReader(is);
            
            ClassWriter writer = InstrumentUtils.newClassWriter();
            
            InstrumentUtils.accept(reader, new ClassRenamer(newUniqueName(clazz), writer));
            
            return writer.toByteArray();
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }
    
    private static String newUniqueName(Class<?> clazz) {
        StringBuilder sb = new StringBuilder();

        sb.append(clazz.getName());
        sb.append("$");
        sb.append(Math.abs(RND.nextInt()));
                
        return sb.toString();
    }
}

package org.gridkit.nimble.btrace.ext;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Observable;
import java.util.Observer;

import net.java.btrace.api.core.BTraceLogger;
import net.java.btrace.api.core.Lookup;
import net.java.btrace.api.wireio.Channel;
import net.java.btrace.api.wireio.Command;
import net.java.btrace.spi.wireio.CommandImpl;

@Command(clazz = ConfigureSessionCmd.class)
public class ConfigureSessionCmdImpl extends CommandImpl<ConfigureSessionCmd> {
    private static final String SESSION_CLASS = "net.java.btrace.agent.Session";
    private static final String CONNECTED_STATE = "CONNECTED";
    private static final String SCRIPT_CLASS_FIELD = "btraceClazz";
    
    @Override
    public void execute(Lookup ctx, ConfigureSessionCmd cmd) {
        String result = null;
        
        Observable session = getSession(ctx);
        
        if (session != null) {
            String scriptClass = getScriptClass(session);
            
            if (scriptClass != null) {
                Observer shutdownObserver = new ShutdownObserver(scriptClass);
                session.addObserver(shutdownObserver);
                result = scriptClass;
            }
        }
        
        Channel channel = ctx.lookup(Channel.class);
        
        try {
            channel.sendResponse(cmd, result);
        } catch (IOException e) {
            BTraceLogger.debugPrint(e);
        }
    }
    
    private Observable getSession(Lookup ctx) {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        
        try {
            Class<?> sessionClass = cl.loadClass(SESSION_CLASS);
            return (Observable)ctx.lookup(sessionClass);
        } catch (ClassNotFoundException e) {
            BTraceLogger.debugPrint(e);
            return null;
        }
    }
    
    private String getScriptClass(Observable session) {
        try {
            Field field = session.getClass().getDeclaredField(SCRIPT_CLASS_FIELD);
            field.setAccessible(true);
            return ((Class<?>)field.get(session)).getName();
        } catch (Exception e) {
            BTraceLogger.debugPrint(e);
            return null;
        }
    }
    
    private static class ShutdownObserver implements Observer {
        private final String scriptClass;
        
        public ShutdownObserver(String scriptClass) {
            this.scriptClass = scriptClass;
        }

        @Override
        public void update(Observable o, Object arg) {
            if (CONNECTED_STATE.toString().equals(arg.toString())) {
                Nimble.removeScriptStore(scriptClass);
            }
        }
    }
}

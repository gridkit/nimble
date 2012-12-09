package org.gridkit.nimble.btrace;

import java.io.File;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import net.java.btrace.ext.Printer;
import net.java.btrace.ext.collections.Collections;

import org.gridkit.nimble.btrace.ext.Nimble;

@SuppressWarnings("serial")
public class BTraceClientSettings implements Serializable {
    private List<Class<?>> extensionClasses = new ArrayList<Class<?>>(Arrays.<Class<?>>asList(
        Nimble.class, Printer.class, Collections.class
    ));
    
    private boolean debug = false;
    
    private boolean dumpClasses = false;
    private String dumpDir = null;
    
    private boolean trackRetransform = false;
    
    private boolean unsafe = false;
    
    private String probeDescPath = ".";
    
    public String getExtensionsPath() {
        return path(jars(extensionClasses));
    }

    public String getAgentPath() {
        return path(jar(net.java.btrace.agent.Main.class));
    }
    
    public String getRuntimePath() {
        return path(jar(net.java.btrace.runtime.BTraceRuntime.class));
    }
    
    public String getDumpDir() {
        return dumpDir == null ? System.getProperty("java.io.tmpdir") + File.pathSeparator + "btrace-dump" : dumpDir;
    }
    
    public boolean isDumpClasses() {
        return dumpClasses;
    }

    public boolean isTrackRetransform() {
        return trackRetransform;
    }

    public boolean isUnsafe() {
        return unsafe;
    }

    public String getProbeDescPath() {
        return probeDescPath;
    }

    public void setExtensionClasses(Class<?>... extensionClasses) {
        setExtensionClasses(Arrays.asList(extensionClasses));
    }
    
    public void setExtensionClasses(List<Class<?>> extensionClasses) {
        this.extensionClasses = extensionClasses;
    }

    public void setDumpClasses(boolean dumpClasses) {
        this.dumpClasses = dumpClasses;
    }

    public void setDumpDir(String dumpDir) {
        this.dumpDir = dumpDir;
    }

    public void setTrackRetransform(boolean trackRetransform) {
        this.trackRetransform = trackRetransform;
    }

    public void setUnsafe(boolean unsafe) {
        this.unsafe = unsafe;
    }

    public void setProbeDescPath(String probeDescPath) {
        this.probeDescPath = probeDescPath;
    }
    
    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    private static String jar(Class<?> clazz) {
        URL location = clazz.getProtectionDomain().getCodeSource().getLocation();

        try {
            return (new File(location.toURI())).getAbsolutePath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static List<String> jars(List<Class<?>> classes) {
        List<String> result = new ArrayList<String>();

        for (Class<?> clazz : classes) {
            result.add(jar(clazz));
        }
        
        return result;
    }
    
    private static String path(Collection<String> jars) {
        jars = new LinkedHashSet<String>(jars);
        
        StringBuilder result = new StringBuilder();
        
        int i = 0;
        for (String jar : jars) {
            result.append(jar);

            if (++i < jars.size()) {
                result.append(File.pathSeparator);
            }
        }
        
        return result.toString();
    }
    
    private static String path(String... args) {
        return path(Arrays.asList(args));
    }
}

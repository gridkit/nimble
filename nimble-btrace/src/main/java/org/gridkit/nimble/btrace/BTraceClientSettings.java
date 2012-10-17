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

@SuppressWarnings("serial")
public class BTraceClientSettings implements Serializable {
    private static Class<?> btraceAgentClass = net.java.btrace.agent.Main.class;
    
    private List<Class<?>> extensionClasses = new ArrayList<Class<?>>();
    
    private boolean dumpClasses = false;
    private String dumpDir = null;
    
    private boolean trackRetransform = false;
    
    private boolean unsafe = false;
    
    private String probeDescPath = ".";
    
    public String getExtensionsPath() {
        return path(jars(extensionClasses));
    }

    public String getAgentPath() {
        return path(jar(btraceAgentClass));
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

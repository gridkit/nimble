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
import java.util.Set;

import net.java.btrace.ext.Printer;
import net.java.btrace.ext.collections.Collections;

import org.gridkit.nimble.btrace.ext.Nimble;

@SuppressWarnings("serial")
public class BTraceClientSettings implements Serializable {
    private Set<Class<?>> extensionClasses = new LinkedHashSet<Class<?>>(Arrays.<Class<?>>asList(
        Nimble.class, Printer.class, Collections.class
    ));
    
    private Set<String> extensionJars = new LinkedHashSet<String>();
    
    private boolean debug = false;
    
    private boolean dumpClasses = false;
    private String dumpDir = null;
    
    private boolean trackRetransform = false;
    
    private boolean unsafe = false;
    
    private String probeDescPath = ".";
        
    public String getExtensionsPath() {
        Set<String> jars = new LinkedHashSet<String>();
        jars.addAll(jars(extensionClasses));
        jars.addAll(extensionJars);
        return path(jars);
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

    public void addExtension(String jarPath) {
        if (!extensionJars.contains(jarPath)) {
            extensionJars.add(jarPath);
        }
    }
    
    public void addExtension(Class<?> clazz) {
        if (!extensionClasses.contains(clazz)) {
            extensionClasses.add(clazz);
        }
    }
    
    public Set<Class<?>> getExtensionClasses() {
        return extensionClasses;
    }

    public void setExtensionClasses(Collection<? extends Class<?>> extensionClasses) {
        this.extensionClasses = new LinkedHashSet<Class<?>>(extensionClasses);
    }

    public Set<String> getExtensionJars() {
        return extensionJars;
    }

    public void setExtensionJars(Collection<? extends String> extensionJars) {
        this.extensionJars = new LinkedHashSet<String>(extensionJars);
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
    
    private static List<String> jars(Collection<? extends Class<?>> classes) {
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

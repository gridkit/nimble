package org.gridkit.nimble.btrace;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class BTraceTestRun {
    @BeforeClass
    public static void beforeClass() throws Exception {
        byte[] bytes = jarFiles("../nimble-btrace/target/classes", "../nimble-btrace/target/test-classes", "../nimble-btrace/src/main/resources");
        File jar = new File("target/nimble-btrace.jar");
        write(jar, bytes);
        addJarToClasspath(jar);
    }
        
    @Test
    public void BTraceClientFactoryTest() throws Exception {  
        runTest(Class.forName("org.gridkit.nimble.btrace.BTraceClientFactoryTest"));
    }
    
    @Test
    public void BTraceDriverTest() throws Exception {
        runTest(Class.forName("org.gridkit.nimble.btrace.BTraceDriverTest"));
    }
    
    private void runTest(Class<?> clazz) {
        JUnitCore junitCore = new JUnitCore();
                
        final List<Failure> failures = new CopyOnWriteArrayList<Failure>();
        
        junitCore.addListener(new RunListener() {
            @Override
            public void testFailure(Failure failure) throws Exception {
                failures.add(failure);
            }
        });
        
        junitCore.run(clazz);
        
        if (!failures.isEmpty()) {
            for (Failure failure : failures) {
                System.err.println("-------- Test Failure " + failure + "--------");
                if (failure.getException() != null) {
                    failure.getException().printStackTrace();
                }
            }
            throw new RuntimeException();
        }
    }

    private static byte[] jarFiles(String... files) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        JarOutputStream jarOut = new JarOutputStream(bos);
        for (String file : files) {
            addFiles(jarOut, "", new File(file));
        }
        jarOut.close();
        return bos.toByteArray();
    }

    private static void addFiles(JarOutputStream jarOut, String base, File path) throws IOException {
        for(File file : path.listFiles()) {
            if (file.isDirectory()) {
                addFiles(jarOut, base + file.getName() + "/", file);
            }
            else {
                JarEntry entry = new JarEntry(base + file.getName());
                entry.setTime(file.lastModified());
                try {
                    jarOut.putNextEntry(entry);
                    copy(new FileInputStream(file), jarOut);
                    jarOut.closeEntry();
                } catch (ZipException ignore) {}
            }
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        try {
            byte[] buf = new byte[1 << 12];
            while(true) {
                int n = in.read(buf);
                if(n >= 0) {
                    out.write(buf, 0, n);
                }
                else {
                    break;
                }
            }
        } finally {
            try {
                in.close();
            }
            catch(Exception e) {
                // ignore
            }
        }
    }
    
    private static void write(File file, byte[] bytes) throws IOException {
        FileOutputStream os = new FileOutputStream(file, false);
        os.write(bytes);
        os.close();
    }
    
    private static void addJarToClasspath(File file) throws Exception {
        URLClassLoader classLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
        
        Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        
        addURL.setAccessible(true);
        
        addURL.invoke(classLoader, file.toURI().toURL());
        
        String classPath = System.getProperty("java.class.path");
        
        classPath += File.pathSeparator + file.getAbsolutePath();
        
        System.setProperty("java.class.path", classPath);
    }
}

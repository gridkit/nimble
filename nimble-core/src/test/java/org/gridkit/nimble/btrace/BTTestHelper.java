package org.gridkit.nimble.btrace;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipException;

import org.gridkit.nimble.btrace.ext.Nimble;

public class BTTestHelper {

	public static boolean isNimbleExpanded() {
		return Nimble.class.getResource("Nimble.class").getProtocol().equals("file");
	}
	
	public static String getNimbleBTraceJar() {
        try {
        	String path = Nimble.class.getResource("Nimble.class").getPath();
        	String cn = "/" + Nimble.class.getName().replace('.', '/') + ".class";
        	String base = path.substring(0, path.length() - cn.length()); 
			byte[] bytes = jarFiles(base);
			File jar = new File("target/nimble-btrace.jar");
			write(jar, bytes);
			JarFile arch = new JarFile(jar);
			if (arch.getJarEntry("META-INF/services/net.java.btrace.api.extensions.BTraceExtension") == null) {
				System.err.println("Missing resources is classpath. Try to clean Eclipse projects.");
				throw new RuntimeException("Missing resources is classpath. Try to clean Eclipse projects.");
			}
			return new File("target/nimble-btrace.jar").getCanonicalPath();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

    private static byte[] jarFiles(String... files) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        JarOutputStream jarOut = new JarOutputStream(bos);
        for (String file : files) {
        	if (!new File(file).exists()) {
        		throw new RuntimeException("Not found: " + file);
        	}
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
}

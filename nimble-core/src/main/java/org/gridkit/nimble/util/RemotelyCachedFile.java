package org.gridkit.nimble.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Remote;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RemotelyCachedFile implements FileResource, Serializable {

	private static final long serialVersionUID = 20140114L;
	
	private transient File local;
	private MasterFile masterFile;
	
	private String fileName;
	private String remoteCachePath;
	
	public RemotelyCachedFile(File file) {
		this(file, "{tmp}/nanocache");
	}

	public RemotelyCachedFile(File file, String remoteCachePath) {
		if (!file.isFile()) {
			throw new IllegalArgumentException("No such file: " + file.getAbsolutePath());
		}
		local = file;
		masterFile = new MasterFileHub(local);
		fileName = file.getName();
		this.remoteCachePath = remoteCachePath;
	}
	
	@Override
	public synchronized File getLocalPath() {
		try {
			if (local != null) {
				return local;
			}
			else {
				String cache = normalizePath(remoteCachePath);
				String hash = masterFile.getHash();
				File path = new File(new File(new File(cache), hash), fileName);
				if (path.exists()) {
					return path;
				}
				String localhost = InetAddress.getLocalHost().getHostName();
				masterFile.pullData(localhost, path.getPath(), new FileSink(path));
				local = path;
				return local;
			}
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// test only
	@Deprecated
	void setCachePath(String path) {
		remoteCachePath = path;
	}
	
	public interface MasterFile extends Remote {
		
		public String getHash();
		
		public void pullData(String host, String path, RemoteFileSink fileSink);
		
	}
	
	private static class MasterFileHub implements MasterFile {
		
		private Map<String, FileStatus> uploads = new HashMap<String, FileStatus>();
		private File file;
		private String hash;

		public MasterFileHub(File file) {
			this.file = file;
		}

		@Override
		public synchronized String getHash() {
			try {
				if (hash == null) {
					hash = digest(new FileInputStream(file), "SHA-1");
				}
				return hash;
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public void pullData(String host, String path, RemoteFileSink fileSink) {
			FileStatus status = slot(host, path);
			synchronized (status) {
				try {
					if (!status.uploaded) {
						byte[] buf = new byte[64 << 10];
						FileInputStream fos = new FileInputStream(file);
						while(true) {
							int n = fos.read(buf);
							if (n < 0) {
								break;
							}
							else {
								if (n < buf.length) {
									fileSink.write(Arrays.copyOf(buf, n));
								}
								else {
									fileSink.write(buf);
								}
							}
						}
						fileSink.close();
						status.uploaded = true;
					}
				} catch (FileNotFoundException e) {
					throw new RuntimeException(e);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}			
		}

		private synchronized FileStatus slot(String host, String path) {
			String key = host + ":" + path;
			if (uploads.containsKey(key)) {
				return uploads.get(key);
			}
			else {
				FileStatus fs = new FileStatus();
				uploads.put(key, fs);
				return fs;
			}
		}
	}
	
	private static class FileStatus {
		
		boolean uploaded;
		
	}
	
	public interface RemoteFileSink extends Remote {
		
		public void write(byte[] data);
		
		public void close();
		
	}
	
	private static class FileSink implements RemoteFileSink {

		private File target;
		private FileOutputStream fos;
		
		public FileSink(File target) {
			this.target = target;
		}

		@Override
		public synchronized void write(byte[] data) {
			try {
				if (fos == null) {
					target.getParentFile().mkdirs();
					fos = new FileOutputStream(tempFile());
				}
				fos.write(data);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		private File tempFile() {
			File f = new File(target.getParentFile(), "part." + target.getName());
			return f;
		}

		@Override
		public synchronized void close() {
			try {
				write(new byte[0]);
				fos.close();
				tempFile().renameTo(target);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * Transforms and conanize path according to local system.
	 * <li>
	 * Expands ~/
	 * </li>
	 * <li>
	 * Replaces {tmp} to local IO temp dir
	 * </li>
	 * 
	 * TODO should use SystemHelper instead
	 *  
	 */
	static String normalizePath(String path) throws IOException {
		if (path.startsWith("~/")) {
			String home = System.getProperty("user.home");
			File fp = new File(new File(home), path.substring("~/".length()));
			return fp.getCanonicalPath();
		}
		else if (path.startsWith("{tmp}/")) {
			File tmp = File.createTempFile("mark", "").getAbsoluteFile();
			tmp.delete();
			File fp = new File(tmp.getParentFile(), path.substring("{tmp}/".length()));
			return fp.getCanonicalPath();
		}
		else {
			return new File(path).getCanonicalPath();
		}
	}		
	
	public static String digest(InputStream is, String algorithm) {
		try {
			MessageDigest md = MessageDigest.getInstance(algorithm);
			byte[] buf = new byte[16 <<10];
			while(true) {
				int n = is.read(buf);
				if (n < 0) {
					is.close();
					break;
				}
				else {
					md.update(buf, 0, n);
				}
			}
			byte[] digest = md.digest();
			StringBuilder sb = new StringBuilder();
			for(byte b: digest) {
				sb.append(Integer.toHexString(0xF & (b >> 4)));
				sb.append(Integer.toHexString(0xF & (b)));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

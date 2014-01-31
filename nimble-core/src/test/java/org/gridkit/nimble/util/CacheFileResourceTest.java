package org.gridkit.nimble.util;

import java.io.File;

import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.vicluster.ViProps;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CacheFileResourceTest {

	public static void rmrf(String path) {
		rmrf(new File(path));
	}

	public static void rmrf(File f) {
		if (f.exists()) {
			if (f.isDirectory()) {
				File[] cc = f.listFiles();
				if (cc != null) {
					for(File c: cc) {
						rmrf(c);
					}
				}
			}
			f.delete();
		}
	}

	public Cloud cloud = CloudFactory.createCloud();
	
	@Before
	public void initCloud() {
		ViProps.at(cloud.node("**")).setIsolateType();
	}

	@Before
	public void cleanup() {
		rmrf("target/file-cache-test");
	}

	@After
	public void stopCloud() {
		cloud.shutdown();
	}
	
	@Test
	public void verify_single_consumer() {
		
		final RemotelyCachedFile file = new RemotelyCachedFile(testFile1(), "target/file-cache-test/A");
		
		cloud.node("Slave").exec(new Runnable() {
			@Override
			public void run() {
				File local = file.getLocalPath();
				Assert.assertTrue(local.isFile());
				local.delete();
				Assert.assertFalse(file.getLocalPath().isFile());
			}
		});
	}

	@Test
	public void verify_couple_consumers_on_one_host() {
		
		final RemotelyCachedFile file = new RemotelyCachedFile(testFile1(), "target/file-cache-test/B");
		
		cloud.node("First").exec(new Runnable() {
			@Override
			public void run() {
				File local = file.getLocalPath();
				Assert.assertTrue(local.isFile());
				local.delete();
				Assert.assertFalse(file.getLocalPath().isFile());
			}
		});

		cloud.node("Second").exec(new Runnable() {
			@Override
			public void run() {
				Assert.assertFalse(file.getLocalPath().isFile());
			}
		});
	}

	@Test
	public void verify_couple_hosts() {
		
		final RemotelyCachedFile file = new RemotelyCachedFile(testFile1(), "target/file-cache-test/C");
		
		cloud.node("First").exec(new Runnable() {
			@Override
			public void run() {
				File local = file.getLocalPath();
				Assert.assertTrue(local.isFile());
				local.delete();
				Assert.assertFalse(file.getLocalPath().isFile());
			}
		});
		
		cloud.node("Second").exec(new Runnable() {
			@Override
			@SuppressWarnings("deprecation")
			public void run() {
				file.setCachePath("target/file-cache-test/D");
				Assert.assertTrue(file.getLocalPath().isFile());
			}
		});
	}

	private File testFile1() {
		return new File("src/test/resources/logback.xml");
	}
	
	
}

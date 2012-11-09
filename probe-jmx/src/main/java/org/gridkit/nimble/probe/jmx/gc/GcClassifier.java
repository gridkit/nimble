package org.gridkit.nimble.probe.jmx.gc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ReflectionException;

import org.gridkit.lab.util.jmx.mxstruct.common.GarbageCollectorMXStruct;
import org.gridkit.lab.util.jmx.mxstruct.common.RuntimeMXStruct;

class GcKnowledgeBase {

	enum PoolType {
		
		EDEN,
		SURVIVOR,
		TENURED,
		PERMANENT
		
	}
	
	private static GcTypeMatcher[] GC_CATALOG = {
		// HotSpot default
		eden("Copy", "Eden Space"),
		survivour("Copy", "Survivor Space"),
		tenured("MarkSweepCompact", "Tenured Gen"),
		permanent("MarkSweepCompact", "Perm Gen"),
		permanent("MarkSweepCompact", "Perm Gen [shared-ro]"),
		permanent("MarkSweepCompact", "Perm Gen [shared-rw]"),

		// HotSpot Parallel Scavenge and Parallel Old GC
		eden("PS Scavenge", "PS Eden Space"),
		survivour("PS Scavenge", "PS Survivor Space"),
		tenured("PS MarkSweep", "PS Old Gen"),
		permanent("PS MarkSweep", "PS Perm Gen"),

		// Concurrent Mark Sweep
		eden("ParNew", "Par Eden Space"),
		survivour("ParNew", "Par Survivor Space"),
		tenured("ConcurrentMarkSweep", "CMS Old Gen"),
		permanent("ConcurrentMarkSweep", "CMS Perm Gen"),

		// G1
		eden("G1 Young Generation", "G1 Eden"),
		survivour("G1 Young Generation", "G1 Survivor"),
		tenured("G1 Old Generation", "G1 Old Gen"),
		permanent("G1 Old Generation", "G1 Perm Gen"),

		// JRockit
		eden("JRockit", "Nursery"),
		// no separate survivor space
		tenured("JRockit", "Old Space"),
		permanent("JRockit", "Class Memory"),
	};
	
	public static Map<PoolType, Collection<String>> classifyMemoryPools(MBeanServerConnection conn) throws IOException {
	
		try {
			boolean jrockit = "Oracle JRockit(R)".equals(RuntimeMXStruct.get(conn).getVmName());
			Map<PoolType, Collection<String>> map = new HashMap<GcKnowledgeBase.PoolType, Collection<String>>();
			for(GarbageCollectorMXStruct gc: GarbageCollectorMXStruct.get(conn).values()) {
				String gcName = jrockit ? "JRockit" : gc.getName();
				for(String pool: gc.getMemoryPoolNames()) {
					PoolType type = classify(gcName, pool);
					if (type != null) {
						add(map, type, pool);
					}
				}
			}
			
			return map;
		} catch (ReflectionException e) {
			throw new IOException(e);
		}
	}
	
	private static PoolType classify(String gcName, String pool) {
		for(GcTypeMatcher m: GC_CATALOG) {
			if (m.gcName.equals(gcName) && m.poolName.equals(pool)) {
				return m.type;
			}
		}
		return null;
	}

	private static void add(Map<PoolType, Collection<String>> map, PoolType type, String name) {
		if (map.containsKey(type)) {
			List<String> names = new ArrayList<String>();
			names.addAll(map.get(type));
			names.add(name);
			map.put(type, names);
		}
		else {
			map.put(type, Collections.singleton(name));
		}
	}
	
	private static GcTypeMatcher eden(String algo, String poolName) {
		return new GcTypeMatcher(algo, poolName, PoolType.EDEN);
	}

	private static GcTypeMatcher survivour(String algo, String poolName) {
		return new GcTypeMatcher(algo, poolName, PoolType.SURVIVOR);
	}

	private static GcTypeMatcher tenured(String algo, String poolName) {
		return new GcTypeMatcher(algo, poolName, PoolType.TENURED);
	}

	private static GcTypeMatcher permanent(String algo, String poolName) {
		return new GcTypeMatcher(algo, poolName, PoolType.PERMANENT);
	}
	
	private static class GcTypeMatcher {
		
		String gcName;
		String poolName;
		PoolType type;
		
		public GcTypeMatcher(String gcName, String poolName, PoolType type) {
			this.gcName = gcName;
			this.poolName = poolName;
			this.type = type;
		}
	}
	
}

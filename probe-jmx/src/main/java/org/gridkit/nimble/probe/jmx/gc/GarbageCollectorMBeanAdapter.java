package org.gridkit.nimble.probe.jmx.gc;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.gridkit.lab.monitoring.probe.SamplerProvider;
import org.gridkit.lab.util.jmx.mxstruct.common.GarbageCollectorMXStruct;
import org.gridkit.lab.util.jmx.mxstruct.common.GarbageCollectorMXStruct.LastGcInfo;
import org.gridkit.lab.util.jmx.mxstruct.common.RuntimeMXStruct;
import org.gridkit.nimble.probe.jmx.MBeanSampler;
import org.gridkit.nimble.probe.jmx.MBeanTarget;
import org.gridkit.nimble.probe.jmx.gc.GarbageCollectionSampler.GcReport;
import org.gridkit.nimble.probe.jmx.gc.GcKnowledgeBase.PoolType;

public class GarbageCollectorMBeanAdapter implements SamplerProvider<MBeanTarget, MBeanSampler>, Serializable {

	private static final long serialVersionUID = 20121106L;
	
	private final SamplerProvider<MBeanTarget, GarbageCollectionSampler> gcSampler;

	public GarbageCollectorMBeanAdapter(SamplerProvider<MBeanTarget, GarbageCollectionSampler> gcSampler) {
		this.gcSampler = gcSampler;
	}

	@Override
	public MBeanSampler getSampler(MBeanTarget target) {
		GarbageCollectionSampler gcs = gcSampler.getSampler(target);
		try {
			return new GcAdapter(target, gcs);
		} catch(RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static class GcAdapter implements MBeanSampler {

		private final GarbageCollectionSampler sampler;
		private final String name;
		private final long processStartMs;
		private final List<String> collectedPools;
		private final List<String> allCollectedPools;
		
		private final List<String> edenPools;
		private final List<String> survivourPools;
		private final List<String> youngPools;
		private final List<String> oldPools;
		@SuppressWarnings("unused")
		private final List<String> permPools;
		
		private final boolean isYoung;
		
		private long gcCount = -1;
		
		
		public GcAdapter(MBeanTarget target, GarbageCollectionSampler sampler) throws ReflectionException, IOException {
			this.sampler = sampler;
			GarbageCollectorMXStruct gc = getGcBean(target.getConnection(), target.getMbeanName());
			name = gc.getName();
			RuntimeMXStruct runtime = RuntimeMXStruct.get(target.getConnection());
			processStartMs = runtime.getStartTime();
			collectedPools = Arrays.asList(gc.getMemoryPoolNames());
			Set<String> allPools = new HashSet<String>();
			for(GarbageCollectorMXStruct gca: GarbageCollectorMXStruct.get(target.getConnection()).values()) {
				allPools.addAll(Arrays.asList(gca.getMemoryPoolNames()));
			}
			allCollectedPools = new ArrayList<String>(allPools);
			
			Map<GcKnowledgeBase.PoolType, Collection<String>> types = GcKnowledgeBase.classifyMemoryPools(target.getConnection());
			
			edenPools = getMemPools(types, PoolType.EDEN);
			survivourPools = getMemPools(types, PoolType.SURVIVOR);
			oldPools = getMemPools(types, PoolType.TENURED);
			permPools = getMemPools(types, PoolType.PERMANENT);
			youngPools = new ArrayList<String>();
			youngPools.addAll(edenPools);
			youngPools.addAll(survivourPools);
			
			isYoung = collectedPools.containsAll(oldPools);
		}

		private List<String> getMemPools(Map<PoolType, Collection<String>> types, PoolType type) {
			List<String> pools;
			if (types.containsKey(type)) {
				pools = new ArrayList<String>(types.get(type));
			}
			else {
				pools = Collections.emptyList();
			}
			return pools;
		}

		@Override
		public void report(MBeanServerConnection connection, ObjectName target) {
			try {
				GarbageCollectorMXStruct gc = getGcBean(connection, target);
				LastGcInfo lastGc = gc.getLastGcInfo();
				if (lastGc.getId() == gcCount) {
					return;
				}
				else {
					long missed = lastGc.getId() - 1 - gcCount;
					if (gcCount < 0) {
						missed = 0;
					}
					gcCount = lastGc.getId();
					
					sampler.report(name, (int)missed, processStartMs + lastGc.getStartTime(), processStartMs +lastGc.getEndTime(), lastGc.getDuration(), new Report(lastGc));
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		private GarbageCollectorMXStruct getGcBean(	MBeanServerConnection connection, ObjectName target) throws ReflectionException, IOException {
			return GarbageCollectorMXStruct.PROTO.read(connection, target);
		}

		private class Report implements GcReport {
			
			private LastGcInfo gcInfo;

			public Report(LastGcInfo gcInfo) {
				this.gcInfo = gcInfo;
			}

			@Override
			public boolean isYoungGC() {
				return isYoung;
			}

			@Override
			public long getCollectedSize() {
				return getTotalSizeBefore() - getTotalSizeAfter();
			}

			@Override
			public long getPromotedSize() {
				return getSizeAfter(oldPools) - getSizeBefore(oldPools);
			}

			@Override
			public long getTotalSizeBefore() {
				return getSizeBefore(allCollectedPools);
			}

			@Override
			public long getTotalSizeAfter() {
				return getSizeAfter(allCollectedPools);
			}

			@Override
			public Collection<String> getColletedPools() {
				return Collections.unmodifiableCollection(collectedPools);
			}
			
			@Override
			public Collection<String> getAllCollectedPools() {
				return Collections.unmodifiableCollection(allCollectedPools);
			}

			@Override
			public Collection<String> getAllMemoryPools() {
				return Collections.unmodifiableCollection(gcInfo.getMemoryUsageAfterGc().keySet());
			}

			@Override
			public long getSizeBefore(String pool) {
				return gcInfo.getMemoryUsageBeforeGc().get(pool).getUsed();
			}

			@Override
			public long getSizeAfter(String pool) {
				return gcInfo.getMemoryUsageAfterGc().get(pool).getUsed();
			}

			@Override
			public long getSizeBefore(Collection<String> pools) {
				long total = 0;
				for(String pool: pools) {
					total += getSizeBefore(pool);
				}
				return total;
			}
			
			@Override
			public long getSizeAfter(Collection<String> pools) {
				long total = 0;
				for(String pool: pools) {
					total += getSizeAfter(pool);
				}
				return total;
			}
		}		
	}
}

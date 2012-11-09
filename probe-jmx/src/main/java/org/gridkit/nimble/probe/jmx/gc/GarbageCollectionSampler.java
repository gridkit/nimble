package org.gridkit.nimble.probe.jmx.gc;

import java.util.Collection;

public interface GarbageCollectionSampler {

	public void report(String algoName, int eventsMissed, long gcStart, long gcFinish, long gcDuration, GcReport info);
	
	interface GcReport {
		
		public boolean isYoungGC();

		public long getCollectedSize();

		public long getPromotedSize();

		public long getTotalSizeBefore();

		public long getTotalSizeAfter();
		
		public Collection<String> getColletedPools();

		public Collection<String> getAllCollectedPools();

		public Collection<String> getAllMemoryPools();

		public long getSizeBefore(String pool);

		public long getSizeAfter(String pool);

		public long getSizeBefore(Collection<String> pools);

		public long getSizeAfter(Collection<String> pools);

	}
}

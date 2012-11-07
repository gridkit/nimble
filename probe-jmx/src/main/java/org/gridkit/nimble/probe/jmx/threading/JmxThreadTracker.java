package org.gridkit.nimble.probe.jmx.threading;

import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;

import org.gridkit.lab.util.jmx.mxstruct.common.ExtendedThreadMXBean;
import org.gridkit.nimble.probe.jmx.MXBeanFactory;

class JmxThreadTracker {

	private ExtendedThreadMXBean mbean;

	private Map<Long, ThreadNote> notes = new HashMap<Long, JmxThreadTracker.ThreadNote>();
	
	public JmxThreadTracker(MBeanServerConnection mserver) {
		this.mbean = MXBeanFactory.newThreadMXBean(mserver);
	}

	public synchronized void updateSnapshot() {
		long[] ids = mbean.getAllThreadIds();
		Arrays.sort(ids);
		long timestamp = System.nanoTime();
		long[] cpuTime = mbean.getThreadCpuTime(ids);
		long[] userTime = mbean.getThreadUserTime(ids);
		long[] alloc = mbean.getThreadAllocatedBytes(ids);
		ThreadInfo[] infos = mbean.getThreadInfo(ids, 0);

		Map<Long, ThreadNote> nnotes = new HashMap<Long, JmxThreadTracker.ThreadNote>();
		for(ThreadInfo i: infos) {
			if (i != null) {
				ThreadNote note = notes.get(i.getThreadId());
				if (note == null) {
					note = new ThreadNote();
					note.threadId = i.getThreadId();
				}
				else {
					note.push();
				}
				
				note.lastTimestamp = timestamp;
				note.lastThreadInfo = i;
				int n = Arrays.binarySearch(ids, i.getThreadId());
				note.lastCpuTime = cpuTime[n];
				note.lastUserTime = userTime[n];
				note.lastMemoryAllocated = alloc[n];
				
				nnotes.put(i.getThreadId(), note);
			}
		}	
		notes = nnotes;
	}
	
	public synchronized List<TradeDetails> getAllThreads() {
		List<TradeDetails> result = new ArrayList<TradeDetails>();
		for(ThreadNote td: notes.values()) {
			if (td.prevThreadInfo != null) {
				result.add(td);
			}
		}
		return result;
	}

	public static interface TradeDetails {
		
		public long getThreadId();
		
		public String getThreadName();
		
		public long getPrevTimestamp();
		
		public long getLastTimestamp();
		
		public long getCpuTime();
		
		public long getUserTime();
		
		public long getAllocatedBytes();
		
		public long getWaitCount();
		
		public long getWaitTime();
		
		public long getBlockedCount();
		
		public long getBlockedTime();
		
	}
	
	private static class ThreadNote implements TradeDetails {
		
		long threadId;
		
		long prevTimestamp;
		long lastTimestamp;
		
		ThreadInfo prevThreadInfo;
		ThreadInfo lastThreadInfo;
		
		long prevCpuTime;
		long lastCpuTime;
		
		long prevUserTime;
		long lastUserTime;
		
		long prevMemoryAllocated;
		long lastMemoryAllocated;
		
		public void push() {
			prevThreadInfo = lastThreadInfo;
			prevCpuTime = lastCpuTime;
			prevUserTime = lastUserTime;
			prevTimestamp = lastTimestamp;
			prevMemoryAllocated = lastMemoryAllocated; 
		}

		@Override
		public long getThreadId() {
			return threadId;
		}

		@Override
		public String getThreadName() {
			return lastThreadInfo.getThreadName();
		}

		@Override
		public long getPrevTimestamp() {
			return prevTimestamp;
		}

		@Override
		public long getLastTimestamp() {
			return lastTimestamp;
		}

		@Override
		public long getCpuTime() {
			return lastCpuTime == -1 ? 0 : lastCpuTime - prevCpuTime;
		}

		@Override
		public long getUserTime() {
			return lastUserTime == -1 ? 0 : lastUserTime - prevUserTime;
		}

		@Override
		public long getAllocatedBytes() {
			return lastMemoryAllocated == -1 ? 0 : lastMemoryAllocated - prevMemoryAllocated;
		}

		@Override
		public long getWaitCount() {
			return lastThreadInfo.getWaitedCount() - prevThreadInfo.getWaitedCount();
		}

		@Override
		public long getWaitTime() {
			return TimeUnit.MILLISECONDS.toNanos(lastThreadInfo.getWaitedTime() - prevThreadInfo.getWaitedTime());
		}

		@Override
		public long getBlockedCount() {
			return lastThreadInfo.getBlockedCount() - prevThreadInfo.getBlockedCount();
		}

		@Override
		public long getBlockedTime() {
			return TimeUnit.MILLISECONDS.toNanos(lastThreadInfo.getBlockedTime() - prevThreadInfo.getBlockedTime());
		}
	}
}

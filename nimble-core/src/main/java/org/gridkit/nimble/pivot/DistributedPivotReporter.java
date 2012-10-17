package org.gridkit.nimble.pivot;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.rmi.Remote;
import java.util.HashMap;
import java.util.Map;

import org.gridkit.nimble.metering.SampleReader;

public class DistributedPivotReporter extends PivotReporter {

	private StatsReceiver receiver = new StatsReceiver() {		
		@Override
		public void accumulate(Map<LevelPath, Map<Object, Object>> subaggregate) {
			DistributedPivotReporter.this.accumulate(subaggregate);			
		}
	};
	
	public DistributedPivotReporter(Pivot pivot) {
		super(pivot);
	}

	public SampleAccumulator createSlaveReporter() {
		return new SlaveReporter(summary.info, receiver);
	}

	synchronized void accumulate(Map<LevelPath, Map<Object, Object>> subaggregate) {
		for(Map.Entry<LevelPath, Map<Object, Object>> row: subaggregate.entrySet()) {
			LevelPath path = row.getKey();
			Map<Object, Object> stats = row.getValue();
			ensurePath(path);
			LevelSummary ls = data.get(path);
			merge(ls, stats);
		}
	}

	private void ensurePath(LevelPath path) {
		if (!data.containsKey(path)) {
			ensurePath(path.parent());
			if (path.isLevel()) {
				data.get(path.parent()).ensureSublevels(this);
			}
			else if (path.isGroup()) {
				createSubGroup(data.get(path.parent()), path.g());
			}
		}		
	}

	private void merge(LevelSummary ls, Map<Object, Object> stats) {
		ls.ensureAggregations();
		for(Object key: ls.aggregations.keySet()) {
			if (stats.containsKey(key)) {
				ls.aggregations.get(key).addAggregate(stats.get(key));
			}
		}
	}


	private interface StatsReceiver extends Remote {
		
		public void accumulate(Map<LevelPath, Map<Object, Object>> subaggregate);
		
	}
	
	private static class SlaveReporter implements SampleAccumulator, Serializable {
	
		private static final long serialVersionUID = 20121014L;
		
		private final LevelInfo rootLevel;
		private final StatsReceiver receiver;

		private transient PivotReporter reporter;
		
		public SlaveReporter(LevelInfo linfo, StatsReceiver receiver) {
			this.rootLevel = linfo;
			this.receiver = receiver;
			this.reporter = new PivotReporter(linfo);
		}

		@Override
		public void accumulate(SampleReader samples) {
			reporter.accumulate(samples);			
		}

		@Override
		public void flush() {
			Map<LevelPath, Map<Object, Object>> aggregate = new HashMap<LevelPath, Map<Object,Object>>();
			for(LevelPath path : reporter.data.keySet()) {
				Map<Object, Object> rowData = reporter.getRowData(path);
				if (rowData != null) {
					aggregate.put(path, rowData);
				}
			}
			receiver.accumulate(aggregate);
			resetStats();		
		}

		private void resetStats() {
			reporter = new PivotReporter(rootLevel);
		}
		
		private void readObject(ObjectInputStream is) throws IOException, ClassNotFoundException {
			is.defaultReadObject();
			reporter = new PivotReporter(rootLevel);
		}		
	}		
}

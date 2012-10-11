package org.gridkit.nimble.pivot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gridkit.nimble.metering.SampleReader;

public class PivotReporter implements SampleAccumulator {

	private LevelSummary summary;
	private Map<RowPath, LevelSummary> data = new HashMap<RowPath, PivotReporter.LevelSummary>(); 
	
	public PivotReporter(Pivot pivot) {
		this(translate(pivot.root()));
	}
	
	private static LevelInfo translate(Pivot.Level level) {
		LevelInfo li = new LevelInfo();
		li.levelId = level.getId();
		li.filter = level.getFilter();
		li.groupBy = level.getGroupping();
		li.aggregators = level.getAggregators();
		li.captureStatics = level.shouldCaptureStatics();
		li.levels = new ArrayList<PivotReporter.LevelInfo>();
		for(Pivot.Level l: level.getSublevels()) {
			li.levels.add(translate(l));
		}
		return li;		
	}

	protected PivotReporter(LevelInfo rootLevel) {
		this.summary = new LevelSummary(null, rootLevel);
		data.put(summary.path, summary);		
	}
	
	public List<RowPath> listChildren(RowPath path) {
		if (path.length() == 0) {
			return Collections.singletonList(summary.path);
		}
		LevelSummary ls = data.get(path);
		if (ls == null) {
			return Collections.emptyList();
		}
		else {
			List<RowPath> paths = new ArrayList<RowPath>();
			if (ls.sublevels != null) {
				for(LevelSummary lss: ls.sublevels.values()) {
					if (lss.aggregations != null) {
						paths.add(lss.path);
					}
				}
			}
			if (ls.subgroups != null) {
				for(LevelSummary lss: ls.subgroups.values()) {
					paths.add(lss.path);
				}
			}
			return paths;
		}
	}
	
	public Map<Object, Object> getRowData(RowPath path) {
		LevelSummary ls = data.get(path);
		if (ls != null && ls.aggregations != null) {
			return ls.getData();
		}
		else {
			return null;
		}
	}
	
	@Override
	public void accumulate(SampleReader samples) {
		SingleSampleReader ssr = new SingleSampleReader(samples);
		while(true) {
			processSample(summary, ssr);
			if (!samples.next()) {
				return;
			}
		}		
	}

	@Override
	public void flush() {
		// do nothing
	}

	/**
	 * @param reader - single sample reader (actual class declaration is intentional)
	 */
	private void processSample(LevelSummary summary, SingleSampleReader reader) {
		if (summary.info.filter.match(reader)) {
			processAggregations(summary, reader);
			if (summary.isGroupping()) {
				processGroups(summary, reader);
			}
			else {
				summary.ensureSublevels(this);
				for(LevelSummary sublevel: summary.sublevels.values()) {
					processSample(sublevel, reader);
				}
			}				
		}		
	}
	
	private void processGroups(LevelSummary summary, SingleSampleReader reader) {
		if (summary.subgroups != null) {
			Pivot.Extractor groupBy = summary.info.groupBy;
			Object group = groupBy.extract(reader);
			LevelSummary subgroup = summary.subgroups.get(group);
			if (subgroup == null) {
				subgroup = new LevelSummary(summary, group);
				data.put(subgroup.path, subgroup);
				summary.subgroups.put(group, subgroup);
			}
			processSample(subgroup, reader);
		}	
	}

	private void processAggregations(LevelSummary summary, SingleSampleReader reader) {
		summary.ensureAggregations();
		for(Object key: summary.aggregations.keySet()) {
			summary.aggregations.get(key).addSamples(reader);
		}
		if (summary.info.captureStatics) {
			for(Object key: reader.keySet()) {
				if (!summary.aggregations.containsKey(key)) {
					ConstantAggregation agg = new ConstantAggregation(Extractors.field(key));
					agg.addSamples(reader);
					summary.aggregations.put(key, agg);
				}
			}
		}
	}

	private static class LevelInfo implements Serializable {
		
		private static final long serialVersionUID = 20121010L;
		
		private int levelId;
		private Pivot.Filter filter;
		private Pivot.Extractor groupBy;
		private boolean captureStatics;		
		private List<LevelInfo> levels;
		private Map<Object, Pivot.Aggregator> aggregators;
	}
	
	private static class LevelSummary {

		private LevelInfo info;
		private RowPath path;
		private Map<Object, Aggregation<?>> aggregations;
		private Map<Object, LevelSummary> sublevels;
		private Map<Object, LevelSummary> subgroups;
		
		public LevelSummary(LevelSummary parent, LevelInfo level) {
			this.info = level;
			this.path = parent == null ? RowPath.root().l(level.levelId) : parent.path.l(level.levelId);
			aggregations = null;
			sublevels = null;
			subgroups = info.groupBy == null ? null : new HashMap<Object, LevelSummary>();
		}

		public LevelSummary(LevelSummary parent, Object groupId) {
			this.info = parent.info;
			this.path = parent.path.g(groupId);
		}
		
		private boolean isGroupping() {
			return subgroups != null;
		}
		
		private void ensureSublevels(PivotReporter parent) {
			if (sublevels == null) {
				sublevels = new LinkedHashMap<Object, LevelSummary>();
				
				for(LevelInfo l: info.levels) {
					int id = l.levelId;
					LevelSummary sl = new LevelSummary(this, l);
					parent.data.put(sl.path, sl);
					sublevels.put(id, sl);
				}
			}
		}
		
		private void ensureAggregations() {
			if (aggregations == null) {
				aggregations = new LinkedHashMap<Object, Aggregation<?>>();
				for(Object key: info.aggregators.keySet()) {
					aggregations.put(key, info.aggregators.get(key).newAggregation());
				}
			}
		}
		
		Map<Object, Object> getData() {
			Map<Object, Object> data = new LinkedHashMap<Object, Object>();
			for(Object key: aggregations.keySet()) {
				data.put(key, aggregations.get(key).getResult());
			}
			return data;
		}
	}
		
	private static class SingleSampleReader implements SampleReader {
	
		private final SampleReader reader;

		public SingleSampleReader(SampleReader reader) {
			this.reader = reader;
		}

		public boolean next() {
			return false;
		}

		public List<Object> keySet() {
			return reader.keySet();
		}

		public Object get(Object key) {
			return reader.get(key);
		}
	}
}

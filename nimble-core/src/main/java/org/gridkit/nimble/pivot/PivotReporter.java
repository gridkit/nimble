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

	protected LevelSummary summary;
	protected Map<LevelPath, LevelSummary> data = new HashMap<LevelPath, PivotReporter.LevelSummary>(); 
	
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
	
	public List<LevelPath> listChildren(LevelPath path) {
		if (path.length() == 0) {
			return Collections.singletonList(summary.path);
		}
		LevelSummary ls = data.get(path);
		if (ls == null) {
			return Collections.emptyList();
		}
		else {
			List<LevelPath> paths = new ArrayList<LevelPath>();
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
	
	public Map<Object, Object> getRowData(LevelPath path) {
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
		if (!samples.isReady() && !samples.next()) {
			return;
		}
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
				subgroup = createSubGroup(summary, group);
			}
			processSample(subgroup, reader);
		}	
	}

	protected LevelSummary createSubGroup(LevelSummary summary, Object group) {
		LevelSummary subgroup;
		subgroup = new LevelSummary(summary, group);
		data.put(subgroup.path, subgroup);
		summary.subgroups.put(group, subgroup);
		return subgroup;
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

	protected static class LevelInfo implements Serializable {
		
		private static final long serialVersionUID = 20121010L;
		
		private int levelId;
		private Pivot.Filter filter;
		private Pivot.Extractor groupBy;
		private boolean captureStatics;		
		private List<LevelInfo> levels;
		private Map<Object, Pivot.Aggregator> aggregators;
	}
	
	protected static class LevelSummary {

		LevelInfo info;
		LevelPath path;
		Map<Object, Aggregation<Object>> aggregations;
		Map<Object, LevelSummary> sublevels;
		Map<Object, LevelSummary> subgroups;
		
		public LevelSummary(LevelSummary parent, LevelInfo level) {
			this.info = level;
			this.path = parent == null ? LevelPath.root().l(level.levelId) : parent.path.l(level.levelId);
			aggregations = null;
			sublevels = null;
			subgroups = info.groupBy == null ? null : new HashMap<Object, LevelSummary>();
		}

		public LevelSummary(LevelSummary parent, Object groupId) {
			this.info = parent.info;
			this.path = parent.path.g(groupId);
		}
		
		boolean isGroupping() {
			return subgroups != null;
		}
		
		void ensureSublevels(PivotReporter parent) {
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
		
		@SuppressWarnings("unchecked")
		void ensureAggregations() {
			if (aggregations == null) {
				aggregations = new LinkedHashMap<Object, Aggregation<Object>>();
				for(Object key: info.aggregators.keySet()) {
					aggregations.put(key, (Aggregation<Object>)info.aggregators.get(key).newAggregation());
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

		@Override
		public boolean isReady() {
			return true;
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

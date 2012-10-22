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

	public static final LevelKey LEVEL_KEY = LevelKey.LEVEL_KEY;
	private static enum LevelKey {
		LEVEL_KEY;
	}
	
	protected LevelSummary summary;
	protected Map<LevelPath, LevelSummary> data = new HashMap<LevelPath, PivotReporter.LevelSummary>(); 
	
	public PivotReporter(Pivot pivot) {
		this(translate(pivot.root(), null));
	}
	
	private static LevelInfo translate(Pivot.Level level, LevelInfo parent) {
		LevelInfo li = new LevelInfo();
		li.levelId = level.getId();
		li.name = level.getName();
		li.parent = parent;
		li.filter = level.getFilter();
		li.groupBy = level.getGroupping();
		li.pivoted = level.isPivoted();
		li.aggregators = level.getAggregators();
		li.captureStatics = level.shouldCaptureStatics();
		li.levels = new ArrayList<PivotReporter.LevelInfo>();
		for(Pivot.Level l: level.getSublevels()) {
			li.levels.add(translate(l, li));
		}
		return li;		
	}

	protected PivotReporter(LevelInfo rootLevel) {
		this.summary = new LevelSummary(null, rootLevel);
		data.put(summary.path, summary);		
	}
	
	public SampleReader getReader() {
		return new LevelReader(LevelPath.root());		
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

	static String combine(String a, String b) {
		if (a == null || a.length() == 0) {
			return b;
		}
		else if (b == null || b.length() == 0) {
			return a;
		}
		else {
			return a + "." + b;
		}
	}
	
	protected static class LevelInfo implements Serializable {
		
		private static final long serialVersionUID = 20121010L;
		
		private String name;
		private LevelInfo parent;
		private int levelId;
		private Pivot.Filter filter;
		private Pivot.Extractor groupBy;
		private boolean captureStatics;
		private boolean pivoted;
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
			if (isGroupping()) {
				return Collections.emptyMap();
			}
			else {
				Map<Object, Object> data = new LinkedHashMap<Object, Object>();
				for(Object key: aggregations.keySet()) {
					data.put(key, aggregations.get(key).getResult());
				}
				return data;
			}
		}
		
		boolean isReportable() {
			if (isGroupping() && sublevels == null) {
				return false;
			}
			else {
				for(LevelSummary sub: sublevels.values()) {
					if (!sub.info.pivoted) {
						return false;
					}
				}
				return true;
			}
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
	
	private class LevelReader implements SampleReader {
		
		private final List<LevelPath> stack = new ArrayList<LevelPath>();
		
		private LevelPath currentRow;
		private Map<Object, Object> bottom;
		private List<Object> keySet;
		
		public LevelReader(LevelPath path) {
			push(path);
		}

		@Override
		public boolean isReady() {
			return currentRow != null;
		}

		@Override
		public boolean next() {
			currentRow = null;
			bottom = null;
			keySet = null;
			while(true) {
				LevelPath path = pop();
				for(LevelPath sub: listChildren(path)) {
					push(sub);
				}
				LevelSummary summary = data.get(path);
				if (summary == null || !summary.isReportable()) {
					// continue
				}
				else {
					currentRow = path;
					bottom = summary.getData();
					return true;
				}
			}
		}

		@Override
		public List<Object> keySet() {
			if (keySet != null) {
				return keySet;
			}
			else {
				keySet = new ArrayList<Object>();
				keySet.add(LEVEL_KEY);
				caclKeySet(keySet, currentRow);
			}
			return keySet;
		}

		private void caclKeySet(List<Object> proto, LevelPath path) {
			if (path != null) {
				caclKeySet(proto, path.parent());
				LevelSummary ls = data.get(path);
				if (ls != null) {
					for(Object key: ls.getData().keySet()) {
						if (!proto.contains(key)) {
							proto.add(key);
						}
					}
				}
			}
		}

		@Override
		public Object get(Object key) {
			if (key == LEVEL_KEY) {
				return getLevelKey();
			}
			else {
				if (bottom.containsKey(key)) {
					return bottom.get(key);
				}
				else {
					return findValue(currentRow.parent(), key);
				}
			}
		}

		private Object getLevelKey() {
			return getLevelKey(currentRow, "");
		}

		private Object getLevelKey(LevelPath path, String suffix) {
			if (path == null) {
				return suffix;
			}
			else {
				LevelSummary ls = data.get(path);
				if (ls != null && !ls.isGroupping() && ls.info.name != null) {
					suffix = combine(ls.info.name, suffix);
				}
				return getLevelKey(path.parent(), suffix);
			}
		}

		private Object findValue(LevelPath path, Object key) {
			if (path != null) {
				LevelSummary ls = data.get(path);
				if (ls !=null) {
					if (ls.aggregations.containsKey(key)) {
						return ls.aggregations.get(key).getResult();
					}
				}
				return findValue(path.parent(), key);
			}
			else {
				return null;			
			}
		}

		private void push(LevelPath path) {
			stack.add(path);
		}
		
		private LevelPath pop() {
			if (stack.isEmpty()) {
				return null;
			}
			else {
				return stack.remove(stack.size() - 1); 
			}
		}
	}
}

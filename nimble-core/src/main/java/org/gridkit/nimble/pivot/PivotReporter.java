package org.gridkit.nimble.pivot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gridkit.nimble.metering.DeltaSampleWriter;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.metering.SampleSet;
import org.gridkit.nimble.statistics.CombinedSummary;
import org.gridkit.nimble.statistics.Summary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PivotReporter implements SampleAccumulator {

	private static final Logger LOGGER = LoggerFactory.getLogger(PivotReporter.class);
	
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
		li.filter = level.getFilter();
		li.groupBy = level.getGroupping();
		li.pivoted = level.isPivoted();
		li.verbatim = level.isVerbatim();
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
	
	protected List<LevelPath> listChildren(LevelPath path) {
		if (path == null) {
			new String();
		}
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
	
	protected Map<?, ?> getRowData(LevelPath path) {
		LevelSummary ls = data.get(path);
		if (ls.isVerbatim()) {
			return Collections.singletonMap(SampleSet.class, ls.samples.createSampleSet());
		}
		else {
			if (ls != null && ls.aggregations != null) {
				return ls.getUnrefinedData();
			}
			else {
				return null;
			}
		}
	}
	
	@Override
	public void accumulate(SampleReader samples) {
		if (!samples.isReady() && !samples.next()) {
			return;
		}
		SingleSampleReader ssr = new SingleSampleReader(samples);
		while(true) {
			if (!processSample(summary, ssr)) {
				LOGGER.debug("Sample ignored: " + printSample(samples));
			}
			if (!samples.next()) {
				return;
			}
		}		
	}

	private String printSample(SampleReader sample) {
		Map<String, Object> line = new LinkedHashMap<String, Object>();
		for(Object key: sample.keySet()) {
			line.put(key.toString(), sample.get(key));
		}
		return line.toString();
	}

	@Override
	public void flush() {
		// do nothing
	}

	/**
	 * @param reader - single sample reader (actual class declaration is intentional)
	 */
	private boolean processSample(LevelSummary summary, SingleSampleReader reader) {
		boolean processed = false;
		if (summary.info.filter.match(reader)) {
			processed |= processAggregations(summary, reader);
			if (summary.isGroupping()) {
				processed |= processGroups(summary, reader);
			}
			else {
				summary.ensureSublevels(this);
				for(LevelSummary sublevel: summary.sublevels.values()) {
					processed |= processSample(sublevel, reader);
				}
			}				
		}		
		return processed;
	}
	
	private boolean processGroups(LevelSummary summary, SingleSampleReader reader) {
		boolean processed = false;
		if (summary.subgroups != null) {
			SampleExtractor groupBy = summary.info.groupBy;
			Object group = groupBy.extract(reader);
			LevelSummary subgroup = summary.subgroups.get(group);
			if (subgroup == null) {
				subgroup = createSubGroup(summary, group);
			}
			processed |= processSample(subgroup, reader);
		}	
		return processed;
	}

	protected LevelSummary createSubGroup(LevelSummary summary, Object group) {
		LevelSummary subgroup;
		subgroup = new LevelSummary(summary, group);
		data.put(subgroup.path, subgroup);
		summary.subgroups.put(group, subgroup);
		return subgroup;
	}

	private boolean processAggregations(LevelSummary summary, SingleSampleReader reader) {
		boolean processed = false;
		if (summary.isVerbatim()) {
			summary.samples.addSamples(reader);
			processed = true;
		}
		else {
			summary.ensureAggregations();
			for(Object key: summary.aggregations.keySet()) {
				try {
					summary.aggregations.get(key).addSamples(reader);					
					processed = true;
				}
				catch(Exception e) {
					LOGGER.warn("Error processing sample " + e.toString());
				}
			}
			if (summary.info.captureStatics) {
				for(Object key: reader.keySet()) {
					if (!summary.aggregations.containsKey(key)) {
						ConstantAggregation agg = new ConstantAggregation(Extractors.field(key));
						try {
							agg.addSamples(reader);
							summary.aggregations.put(key, agg);
							processed = true;
						}
						catch(Exception e) {
							LOGGER.warn("Error processing sample " + e.toString());
						}
					}
				}
			}
		}
		return processed;
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
		private int levelId;
		private SampleFilter filter;
		private SampleExtractor groupBy;
		private boolean captureStatics;
		private boolean pivoted;
		private boolean verbatim;
		private List<LevelInfo> levels;
		private Map<Object, Pivot.AggregationFactory> aggregators;
	}
	
	protected static class LevelSummary {

		LevelInfo info;
		LevelPath path;
		Map<Object, Aggregation<Object>> aggregations;
		Map<Object, LevelSummary> sublevels;
		Map<Object, LevelSummary> subgroups;
		DeltaSampleWriter samples; // raw samples;
		
		public LevelSummary(LevelSummary parent, LevelInfo level) {
			this.info = level;
			this.path = parent == null ? LevelPath.root().l(level.levelId) : parent.path.l(level.levelId);
			aggregations = null;
			sublevels = null;
			subgroups = info.groupBy == null ? null : new HashMap<Object, LevelSummary>();
			samples = info.verbatim ? new DeltaSampleWriter() : null;
		}

		public LevelSummary(LevelSummary parent, Object groupId) {
			this.info = parent.info;
			this.path = parent.path.g(groupId);
		}
		
		boolean isGroupping() {
			return subgroups != null;
		}
		
		boolean isVerbatim() {
			return samples != null;
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

		Map<Object, Object> getUnrefinedData() {
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

		@SuppressWarnings("rawtypes")
		Map<Object, Object> getRefinedData() {
			if (isGroupping()) {
				return Collections.emptyMap();
			}
			else {
				Map<Object, Object> data = new LinkedHashMap<Object, Object>();
				for(Object key: aggregations.keySet()) {
					data.put(key, aggregations.get(key).getResult());
					if (key instanceof AggregationKey) {
						Aggregation<?> ag = (Aggregation<?>) aggregations.get(key);
						Object m = ((AggregationKey) key).getMeasureKey();
						Class<? extends Summary> t = ((AggregationKey) key).getSummaryType();
						
						CombinedSummary sa = (CombinedSummary) data.get(Measure.summary(m));
						if (sa == null) {
							sa = new CombinedSummary();
							data.put(Measure.summary(m), sa);
						}
						
						sa.addAggregation(t, (Summary)((Aggregation)ag).getResult());
					}
				}
				return data;
			}
		}

		Map<Object, Object> getDataWithPivot() {
			if (isGroupping()) {
				return Collections.emptyMap();
			}
			else {
				Map<Object, Object> data = getRefinedData();
				if (sublevels != null) {
					for(LevelSummary sub: sublevels.values()) {
						if (sub.info.pivoted) {
							Object levelId = sub.info.name;
							List<Object> deco = isEmpty(levelId) ? Collections.emptyList() : Collections.singletonList(levelId);
							sub.dumpData(data, deco);
						}
					}
				}
				return data;
			}
		}

		private boolean isEmpty(Object levelId) {
			return levelId == null || String.valueOf(levelId).length() == 0;
		}
		
		@SuppressWarnings("rawtypes")
		private void dumpData(Map<Object, Object> data, List<Object> deco) {
			if (aggregations != null) {
				for(Object key: aggregations.keySet()) {
					data.put(new Decorated(deco, key), aggregations.get(key).getResult());
					
					if (key instanceof AggregationKey) {
						Aggregation<?> ag = (Aggregation<?>) aggregations.get(key);
						Object m = ((AggregationKey) key).getMeasureKey();
						Class<? extends Summary> t = ((AggregationKey) key).getSummaryType();
						
						Decorated summaryKey = new Decorated(deco, Measure.summary(m));
						CombinedSummary sa = (CombinedSummary) data.get(summaryKey);
						if (sa == null) {
							sa = new CombinedSummary();
							data.put(summaryKey, sa);
						}
						
						sa.addAggregation(t, (Summary)((Aggregation)ag).getResult());
					}

				}
			}
			if (sublevels != null) {
				for(LevelSummary sub: sublevels.values()) {
					if (sub.info.pivoted) {
						List<Object> subDeco = new ArrayList<Object>(deco);
						Object levelId = sub.info.name;
						if (!isEmpty(levelId)) {
							subDeco.add(levelId);
						}
						sub.dumpData(data, subDeco);
					}
				}
			}
			if (subgroups != null) {
				for(Object g: subgroups.keySet()) {
					LevelSummary sub = subgroups.get(g);
					if (sub.info.pivoted) {
						List<Object> subDeco = new ArrayList<Object>(deco);
						Object levelId = g;
						if (!isEmpty(levelId)) {
							subDeco.add(levelId);
						}
						sub.dumpData(data, subDeco);
					}
				}				
			}
		}

		boolean isReportable() {
			if (isGroupping() || aggregations == null || info.pivoted) {
				return false;
			}
			else {
				for(LevelInfo sub: info.levels) {
					if (!sub.pivoted) {
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
				if (path == null) {
					return false;
				}
				for(LevelPath sub: listChildren(path)) {
					push(sub);
				}
				LevelSummary summary = data.get(path);
				if (summary == null || !summary.isReportable()) {
					// continue
				}
				else {
					currentRow = path;
					bottom = summary.getDataWithPivot();
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
					for(Object key: ls.getDataWithPivot().keySet()) {
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

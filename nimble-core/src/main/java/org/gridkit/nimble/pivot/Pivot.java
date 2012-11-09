package org.gridkit.nimble.pivot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class Pivot {

	private List<Level> levels = new ArrayList<Pivot.Level>();
	
	public Pivot() {	
		new Level();
	}
	
	
	public Level root() {
		return getLevel(0);
	}

	public Level getLevel(int id) {
		return levels.get(id);
	}

	
	public Level level(String name) {
		return root().level(name);
	}
	
	
	public class Level {
		
		private Level parent;
		private int levelId = levels.size();
		private String name;
		private boolean pivoted;
		private boolean verbatim;
		private boolean captureStatics;
		private List<SampleFilter> levelFilters = new ArrayList<SampleFilter>();
		private SampleExtractor groupBy;
		private List<Level> sublevels = new ArrayList<Level>();
		private Map<Object, AggregationFactory> aggregators = new LinkedHashMap<Object, Pivot.AggregationFactory>();
		
		{
			levels.add(this);
		}
		
		public Level level(String name) {
			Level sublevel = new Level();
			sublevel.parent = parent;			
			sublevel.name = name;
			sublevel.pivoted = this.pivoted;
			
			sublevels.add(sublevel);
			
			return sublevel;
		}
		
		public Level group(Object key) {
			Level group = group(Extractors.field(key));
			group.calcConstant(key);
			return group;
		}

		public Level group(SampleExtractor extractor) {
			Level level = level("");
			level.groupBy = extractor;			
			return level;
		}

		public Level group(Object key, SampleExtractor extractor) {
			Level group = group(extractor);
			group.calcConstant(key, extractor);
			return group;
		}
		
		public Level filter(Object key, Object value) {
			return filter(Filters.equals(key, value));
		}

		public Level filter(SampleFilter equals) {
			levelFilters.add(equals);
			return this;
		}

		public Level calcDistribution(Object key) {
			AggregationFactory agg = PivotHelper.createGaussianAggregator(Extractors.field(key));
			addAggregator(AggregationKey.distribution(key), agg);
			return this;
		}
		
        public Level calcDistribution(Object key, SampleExtractor extractor) {
            AggregationFactory agg = PivotHelper.createGaussianAggregator(extractor);
            addAggregator(AggregationKey.distribution(key), agg);
            return this;
        }

		private AggregationFactory addAggregator(Object key, AggregationFactory agg) {
			return aggregators.put(key, agg);
		}

		public Level calcFrequency(Object key) {
			AggregationFactory agg = PivotHelper.createFrequencyAggregator(Extractors.field(key));
			addAggregator(AggregationKey.frequency(key), agg);
			return this;
		}

		public Level calcFrequency(Object key, double weight) {
			AggregationFactory agg = PivotHelper.createFrequencyAggregator(Extractors.constant(weight));
			addAggregator(AggregationKey.frequency(key), agg);
			return this;
		}

		public Level calcFrequency(Object key, SampleExtractor extractor) {
			AggregationFactory agg = PivotHelper.createFrequencyAggregator(extractor);
			addAggregator(AggregationKey.frequency(key), agg);
			return this;
		}

		public Level calcDistinct(Object key) {
			calcDistinct(key, Extractors.field(key));
			return this;
		}

		public Level calcDistinct(Object key, SampleExtractor extractor) {
			AggregationFactory agg = PivotHelper.createDistictAggregator(extractor);
			addAggregator(AggregationKey.distinct(key), agg);
			return this;
		}

		public Level calcConstant(Object key) {
			aggregators.put(key, PivotHelper.createConstantAggregator(Extractors.field(key)));
			return this;
		}

		public Level calcConstant(Object key, SampleExtractor extractor) {
			aggregators.put(key, PivotHelper.createConstantAggregator(extractor));
			return this;
		}

		public Level setConstant(Object key, Object value) {
			aggregators.put(key, PivotHelper.createStaticValue(value));
			return this;
		}
				
		public Level pivot() {
			Level level = level("");
			level.pivoted = true;
			return level;
		}

		@SuppressWarnings("unused")
		@Deprecated
		private void verbatim(String name) {
			Level level = level(name);
			level.verbatim = true;
		}

		@SuppressWarnings("unused")
		@Deprecated
		private void verbatim() {
			Level level = level(name);
			level.verbatim = true;
		}

		public int getId() {
			return levelId;
		}
		
		public String getName() {
			return name;
		}
		
		public SampleFilter getFilter() {
			return Filters.and(levelFilters);
		}
		
		public SampleExtractor getGroupping() {
			return groupBy;
		}
		
		public List<Level> getSublevels() {
			return Collections.unmodifiableList(sublevels);
		}
		
		public Map<Object, AggregationFactory> getAggregators() {
			return Collections.unmodifiableMap(aggregators);
		}
				
		public boolean shouldCaptureStatics() {
			return captureStatics;
		}
		
		public boolean isPivoted() {
			return pivoted;
		}

		public boolean isVerbatim() {
			return verbatim;
		}
	}
	
	public interface AggregationFactory extends Serializable {
		public Aggregation<?> newAggregation();
	}
}

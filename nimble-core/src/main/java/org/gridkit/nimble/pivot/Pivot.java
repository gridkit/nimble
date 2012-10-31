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
		private boolean captureStatics;
		private List<SampleFilter> levelFilters = new ArrayList<SampleFilter>();
		private SampleExtractor groupBy;
		private List<Level> sublevels = new ArrayList<Level>();
		private Map<Object, Aggregator> aggregators = new LinkedHashMap<Object, Pivot.Aggregator>();
		
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
		
		public Level filter(Object key, Object value) {
			return filter(Filters.equals(key, value));
		}

		public Level filter(SampleFilter equals) {
			levelFilters.add(equals);
			return this;
		}

		public Level calcDistribution(Object key) {
			Aggregator agg = PivotHelper.createGaussianAggregator(Extractors.field(key));
			addAggregator(key, agg);
			return this;
		}
		
        public Level calcDistribution(Object key, SampleExtractor extractor) {
            Aggregator agg = PivotHelper.createGaussianAggregator(extractor);
            addAggregator(key, agg);
            return this;
        }

		private Aggregator addAggregator(Object key, Aggregator agg) {
			return aggregators.put(key, agg);
		}

		public Level calcFrequency(Object key) {
			Aggregator agg = PivotHelper.createFrequencyAggregator(Extractors.field(key));
			addAggregator(key, agg);
			return this;
		}

		public Level calcFrequency(Object key, double weight) {
			Aggregator agg = PivotHelper.createFrequencyAggregator(Extractors.constant(weight));
			addAggregator(key, agg);
			return this;
		}

		public Level calcConstant(Object key) {
			aggregators.put(key, PivotHelper.createConstantAggregator(Extractors.field(key)));
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
		
		public Map<Object, Aggregator> getAggregators() {
			return Collections.unmodifiableMap(aggregators);
		}
				
		public boolean shouldCaptureStatics() {
			return captureStatics;
		}
		
		public boolean isPivoted() {
			return pivoted;
		}
	}
	
	public interface Aggregator extends Serializable {
		public Aggregation<?> newAggregation();
	}
}

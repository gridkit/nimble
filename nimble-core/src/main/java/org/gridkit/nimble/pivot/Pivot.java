package org.gridkit.nimble.pivot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gridkit.nimble.metering.SampleReader;

public class Pivot {

	private List<Level> levels = new ArrayList<Pivot.Level>();
	
	public Pivot() {	
		new Level();
	}
	
	
	public Level getRoot() {
		return getLevel(0);
	}

	public Level getLevel(int id) {
		return levels.get(id);
	}

	
	public Level level(String name) {
		return getRoot().level(name);
	}
	
	
	public class Level {
		
		private int levelId = levels.size();
		private String name;
		private boolean pivoted;
		private boolean captureStatics;
		private List<Filter> levelFilters = new ArrayList<Pivot.Filter>();
		private Extractor groupBy;
		private List<Level> sublevels = new ArrayList<Level>();
		private Map<Object, Aggregator> aggregators = new LinkedHashMap<Object, Pivot.Aggregator>();
		private List<Object> displayOrder = new ArrayList<Object>();
		
		{
			levels.add(this);
		}
		
		public Level level(String name) {
			Level sublevel = new Level();
			sublevel.name = name;
			sublevel.pivoted = this.pivoted;
			
			sublevels.add(sublevel);
			displayOrder.add(sublevel);
			
			return sublevel;
		}
		
		public Level group(Object key) {
			return group(Extractors.field(key));
		}

		public Level group(Extractor extractor) {
			Level level = level("");
			level.groupBy = extractor;			
			return level;
		}
		
		public Level filter(Object key, Object value) {
			return filter(Filters.equals(key, value));
		}

		public Level filter(Filter equals) {
			levelFilters.add(equals);
			return this;
		}

		public Level calcGausian(Object key) {
			Aggregator agg = PivotHelper.createGaussianAggregator(Extractors.field(key));
			aggregators.put(key, agg);
			return this;
		}

		public Level calcBuckets(Object key) {
			Aggregator agg = PivotHelper.createDiscretHistogramAggregator(Extractors.field(key));
			aggregators.put(key, agg);
			return this;
		}

		public Level calcFrequency(Object key) {
			Aggregator agg = PivotHelper.createFrequencyAggregator(Extractors.field(key));
			aggregators.put(key, agg);
			return this;
		}
		
		public Level display(Object key) {
			DisplayFunction df = PivotHelper.displayField(key);
			addDisplayFunction(df);
			return this;
		}

		public Level displayStats(Object key) {
			DisplayFunction df = PivotHelper.displayDistributionStats(key);
			addDisplayFunction(df);
			return this;
		}
		
		private void addDisplayFunction(DisplayFunction df) {
			displayOrder.add(df);
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
		
		public Pivot.Filter getFilter() {
			return Filters.and(levelFilters);
		}
		
		public Pivot.Extractor getGroupping() {
			return groupBy;
		}
		
		public List<Level> getSublevels() {
			return Collections.unmodifiableList(sublevels);
		}
		
		public Map<Object, Aggregator> getAggregators() {
			return Collections.unmodifiableMap(aggregators);
		}
		
		public List<Object> getDisplayOrder() {
			return Collections.unmodifiableList(displayOrder);
		}
		
		public boolean shouldCaptureStatics() {
			return captureStatics;
		}
	}
	
	public class FilterBuilder {
		
	}
	
	public interface Filter extends Serializable {
		public boolean match(SampleReader sample);
	}

	public interface Extractor extends Serializable {
		public Object extract(SampleReader sample);
	}
	
	public interface Aggregator extends Serializable {
		public Aggregation<?> newAggregation();
	}
	
}

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
		private boolean visible;
		private boolean captureStatics;
		private List<Filter> levelFilters = new ArrayList<Pivot.Filter>();
		private Extractor groupBy;
		private List<Level> sublevels = new ArrayList<Level>();
		private Map<Object, Aggregator> aggregators = new LinkedHashMap<Object, Pivot.Aggregator>();
		private List<DisplayFunction> displayFunctions = new ArrayList<DisplayFunction>();
		private List<Object> displayOrder = new ArrayList<Object>();
		
		{
			levels.add(this);
		}
		
		public Level level(String name) {
			Level sublevel = new Level();
			sublevel.parent = parent;			
			sublevel.name = name;
			sublevel.pivoted = this.pivoted;
			
			sublevels.add(sublevel);
			displayOrder.add(sublevel);
			
			return sublevel;
		}
		
		public Level group(Object key) {
			Level group = group(Extractors.field(key));
			group.calcConstant(key);
			return group;
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

		public Level calcDistribution(Object key) {
			Aggregator agg = PivotHelper.createGaussianAggregator(Extractors.field(key));
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
		
		public Level display(Object key) {
			DisplayFunction df = PivotHelper.displayField(key);
			addDisplayFunction(df);
			return this;
		}

		public Level displayDistribution(Object key) {
			DisplayFunction df = PivotHelper.displayDistributionStats(key);
			addDisplayFunction(df);
			return this;
		}

		public Level displayThroughput(Object key) {
			DisplayFunction df = PivotHelper.displayFrequency(key);
			addDisplayFunction(df);
			return this;			
		}
		
		public Level displayDistribution(Object key, CommonStats.StatAppraisal... measures) {
			DisplayFunction df = PivotHelper.displayDistributionStats(key, measures);
			addDisplayFunction(df);
			return this;
		}
		
		private void addDisplayFunction(DisplayFunction df) {
			displayFunctions.add(df);
			displayOrder.add(df);
		}
		
		public Level pivot() {
			Level level = level("");
			level.pivoted = true;
			return level;
		}

		public Level show() {
			visible = true;
			return this;
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
		
		public List<DisplayFunction> getAllDisplayFunction() {
			List<DisplayFunction> functions = new ArrayList<DisplayFunction>();
			collectDisplayFunctions(functions);
			return functions;
		}
		
		private void collectDisplayFunctions(List<DisplayFunction> functions) {
			if (parent != null) {
				parent.collectDisplayFunctions(functions);
			}
			functions.addAll(displayFunctions);
		}

		public List<Object> getDisplayOrder() {
			return Collections.unmodifiableList(displayOrder);
		}
		
		public boolean shouldCaptureStatics() {
			return captureStatics;
		}
		
		public boolean isVisible() {
			return visible;
		}

		public boolean isPivoted() {
			return pivoted;
		}
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

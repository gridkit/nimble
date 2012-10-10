package org.gridkit.nimble.pivot;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.pivot.Pivot.Extractor;
import org.gridkit.nimble.pivot.Pivot.Filter;

public class Filters {

	public static Pivot.Filter equals(Object key, Object value) {
		return new EqualsFilter(Extractors.field(key), value);
	}
	
	public static Filter always() {
		return TrueFilter.TRUE;
	}

	public static Filter and(Filter... filters) {
		if (filters.length == 0) {
			return TrueFilter.TRUE;
		}
		return new AndFilter(filters);
	}

	public static Filter and(Collection<Filter> levelFilters) {
		return and(levelFilters.toArray(new Filter[0]));
	}

	static abstract class ExtractorFilter implements Pivot.Filter {
		
		protected final Pivot.Extractor extractor;

		protected ExtractorFilter(Extractor extractor) {
			this.extractor = extractor;
		}

		@Override
		public boolean match(SampleReader sample) {
			return evaluate(extractor.extract(sample));
		}
		
		protected abstract boolean evaluate(Object extract);
	}
	
	
	public static class EqualsFilter extends ExtractorFilter {

		protected Object value;
		
		public EqualsFilter(Extractor extractor, Object value) {
			super(extractor);
			this.value = value;
		}

		@Override
		protected boolean evaluate(Object extract) {
			if (value == null && extract == null) {
				return true;
			}
			else if (value == null) {
				return false;
			}
			else {
				return value.equals(extract);
			}
		}
	}

	public static class AndFilter implements Pivot.Filter, Serializable {

		private final Pivot.Filter[] filters;
		
		public AndFilter(Filter[] filters) {
			this.filters = filters;
		}

		@Override
		public boolean match(SampleReader sample) {
			for(Pivot.Filter f: filters) {
				if (!f.match(sample)) {
					return false;
				}
			}
			return true;
		}
	}

	public static enum TrueFilter implements Pivot.Filter, Serializable {

		TRUE
		
		;
		
		@Override
		public boolean match(SampleReader sample) {
			return true;
		}
	}
}

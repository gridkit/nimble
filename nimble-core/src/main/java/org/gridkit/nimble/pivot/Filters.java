package org.gridkit.nimble.pivot;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

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
	
	public static Filter in(Object key, Collection<? extends Object> values) {
	    return new InFilter(Extractors.field(key), values);
	}

	public static Filter in(Object key, Object... values) {
	    return new InFilter(Extractors.field(key), Arrays.asList(values));
	}
	
	public static Filter and(Collection<Filter> levelFilters) {
		return and(levelFilters.toArray(new Filter[0]));
	}

	static abstract class ExtractorFilter implements Pivot.Filter {
		
		private static final long serialVersionUID = 20121014L;

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

		private static final long serialVersionUID = 20121014L;
		
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

		private static final long serialVersionUID = 20121014L;
		
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

	public static class InFilter extends ExtractorFilter {
	    
        private static final long serialVersionUID = 1112015125669341187L;
        
        private final Collection<? extends Object> values;

        public InFilter(Extractor extractor, Collection<? extends Object> values) {
            super(extractor);
            this.values = values;
        }


        @Override
        protected boolean evaluate(Object extract) {
            return values.contains(extract);
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

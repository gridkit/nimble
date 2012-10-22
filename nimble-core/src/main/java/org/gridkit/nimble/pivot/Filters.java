package org.gridkit.nimble.pivot;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

import org.gridkit.nimble.metering.SampleReader;

public class Filters {

	public static SampleFilter equals(Object key, Object value) {
		return new EqualsFilter(Extractors.field(key), value);
	}

	public static SampleFilter notNull(Object key) {
		return new NotNullFilter(Extractors.field(key));
	}
	
	public static SampleFilter always() {
		return TrueFilter.TRUE;
	}

	public static SampleFilter and(SampleFilter... filters) {
		if (filters.length == 0) {
			return TrueFilter.TRUE;
		}
		return new AndFilter(filters);
	}
	
	public static SampleFilter in(Object key, Collection<? extends Object> values) {
	    return new InFilter(Extractors.field(key), values);
	}

	public static SampleFilter in(Object key, Object... values) {
	    return new InFilter(Extractors.field(key), Arrays.asList(values));
	}
	
	public static SampleFilter and(Collection<SampleFilter> levelFilters) {
		return and(levelFilters.toArray(new SampleFilter[0]));
	}

	static abstract class ExtractorFilter implements SampleFilter {
		
		private static final long serialVersionUID = 20121014L;

		protected final SampleExtractor extractor;

		protected ExtractorFilter(SampleExtractor extractor) {
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
		
		public EqualsFilter(SampleExtractor extractor, Object value) {
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

	public static class NotNullFilter extends ExtractorFilter {
		
		private static final long serialVersionUID = 20121014L;
		
		public NotNullFilter(SampleExtractor extractor) {
			super(extractor);
		}
		
		@Override
		protected boolean evaluate(Object extract) {
			return extract != null;
		}
	}

	public static class AndFilter implements SampleFilter, Serializable {

		private static final long serialVersionUID = 20121014L;
		
		private final SampleFilter[] filters;
		
		public AndFilter(SampleFilter[] filters) {
			this.filters = filters;
		}

		@Override
		public boolean match(SampleReader sample) {
			for(SampleFilter f: filters) {
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

        public InFilter(SampleExtractor extractor, Collection<? extends Object> values) {
            super(extractor);
            this.values = values;
        }


        @Override
        protected boolean evaluate(Object extract) {
            return values.contains(extract);
        }
	}
	
	public static enum TrueFilter implements SampleFilter, Serializable {

		TRUE
		
		;
		
		@Override
		public boolean match(SampleReader sample) {
			return true;
		}
	}
}

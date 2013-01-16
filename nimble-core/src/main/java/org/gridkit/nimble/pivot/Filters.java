package org.gridkit.nimble.pivot;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gridkit.nimble.metering.SampleReader;

public class Filters {

	public static SampleFilter equals(Object key, Object value) {
		return new EqualsFilter(Extractors.field(key), value);
	}

	public static SampleFilter eq(Object key, Object value) {
		return new EqualsFilter(Extractors.field(key), value);
	}

	public static SampleFilter ne(Object key, Object value) {
		return new NotEqualsFilter(Extractors.field(key), value);
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

	public static SampleFilter or(SampleFilter... filters) {
		if (filters.length == 0) {
			return FalseFilter.FALSE;
		}
		return new OrFilter(filters);
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

	public static SampleFilter dotGlob(Object key, String... patterns) {
		return dotGlob(key, Arrays.asList(patterns));
	}

	public static SampleFilter dotGlob(Object key, Collection<String> patterns) {
		if (patterns.isEmpty()) {
			throw new IllegalArgumentException("Pattern list is empty");
		}
		StringBuilder pattern = new StringBuilder();
		for(String p: patterns) {
			Pattern g = GlobHelper.translate(p, ".");
			pattern.append("(");
			pattern.append(g.pattern());
			pattern.append(")|");
		}
		pattern.setLength(pattern.length() - 1);
		return new RegExMatcherFilter(Extractors.field(key), Pattern.compile(pattern.toString()));
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

	public static class RegExMatcherFilter extends ExtractorFilter {
		
		private static final long serialVersionUID = 20121014L;
		
		protected Pattern pattern;
		
		public RegExMatcherFilter(SampleExtractor extractor, Pattern pattern) {
			super(extractor);
			this.pattern = pattern;
		}
		
		@Override
		protected boolean evaluate(Object extract) {
			if (extract instanceof String) {
				Matcher matcher = pattern.matcher((String)extract);
				return matcher.matches();
			}
			else {
				return false;
			}
		}
	}

	public static class NotEqualsFilter extends EqualsFilter {
		
		private static final long serialVersionUID = 20121014L;
		
		public NotEqualsFilter(SampleExtractor extractor, Object value) {
			super(extractor, value);
		}
		
		@Override
		protected boolean evaluate(Object extract) {
			return !super.evaluate(extract);
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

	public static class OrFilter implements SampleFilter, Serializable {
		
		private static final long serialVersionUID = 20121014L;
		
		private final SampleFilter[] filters;
		
		public OrFilter(SampleFilter[] filters) {
			this.filters = filters;
		}
		
		@Override
		public boolean match(SampleReader sample) {
			for(SampleFilter f: filters) {
				if (f.match(sample)) {
					return true;
				}
			}
			return false;
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

	public static enum FalseFilter implements SampleFilter, Serializable {
		
		FALSE
		
		;
		
		@Override
		public boolean match(SampleReader sample) {
			return false;
		}
	}
}

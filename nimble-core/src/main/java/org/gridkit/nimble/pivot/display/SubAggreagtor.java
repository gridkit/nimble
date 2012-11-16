package org.gridkit.nimble.pivot.display;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.pivot.Aggregation;
import org.gridkit.nimble.pivot.Decorated;
import org.gridkit.nimble.pivot.Pivot.AggregationFactory;
import org.gridkit.nimble.pivot.SampleExtractor;
import org.gridkit.nimble.statistics.CombinedSummary;
import org.gridkit.nimble.statistics.Summary;

public class SubAggreagtor implements SampleExtractor, Serializable {

	private static final long serialVersionUID = 20121114L;
	
	private final AggregationFactory aggregation;

	public SubAggreagtor(AggregationFactory aggregation) {
		this.aggregation = aggregation;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object extract(SampleReader sample) {
		Aggregation<Object> agg = (Aggregation<Object>) aggregation.newAggregation();
		Set<Object> keys = new HashSet<Object>();
		for(Object key: sample.keySet()) {
			if (key instanceof Decorated) {
				Decorated dk = (Decorated)key;
				if (dk.getDecoration().size() > 0) {
					keys.add(dk.getDecoration().get(0));
				}
			}
		}
		for(Object key: keys) {
			FilterReader fr = new FilterReader(key, sample);
			agg.addSamples(fr);
		}
		Summary res = (Summary)agg.getResult();
		CombinedSummary sum = new CombinedSummary();
		sum.addAggregation(res.getClass(), res);
		
		return sum;
	}
	
	private static class FilterReader implements SampleReader {

		private Object key;
		private SampleReader reader;
		private List<Object> keySet;
		
		public FilterReader(Object key, SampleReader reader) {
			this.key = key;
			this.reader = reader;
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public boolean next() {
			return false;
		}

		@Override
		public List<Object> keySet() {
			if (keySet == null) {
				LinkedHashSet<Object> set = new LinkedHashSet<Object>();
				for(Object k: reader.keySet()) {
					if (k instanceof Decorated) {
						Decorated dk = (Decorated) k;
						if (dk.startsWith(Collections.singletonList(k))) {
							set.add(Decorated.undecorate(dk));
						}
					}
					else if (!set.contains(k)) {
						set.add(k);
					}
				}
				keySet = new ArrayList<Object>(set);
			}
			
			return keySet;
		}

		@Override
		public Object get(Object k) {
			Decorated dk = Decorated.decorate(key, k);
			Object v = reader.get(dk);
			if (v == null) {
				v = reader.get(k);
			}
			return v;
		}
	}
}

package org.gridkit.nimble.pivot.display;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.pivot.Decorated;

public class DecorationAdapter implements DisplayComponent {

	private final String captionFormat;
	private final List<Object> deco;
	private final DisplayComponent nested;
	
	public DecorationAdapter(List<Object> deco, DisplayComponent nested) {
		this("%s", deco, nested);
	}	
	
	public DecorationAdapter(String captionFormat, List<Object> deco, DisplayComponent nested) {
		this.captionFormat = captionFormat;
		this.deco = deco;
		this.nested = nested;
	}

	@Override
	public Map<String, Object> display(SampleReader reader) {
		DecoReader dr = new DecoReader(reader);
		Map<String, Object> result = nested.display(dr);
		if (!"%s".equals(captionFormat)) {
			Map<String, Object> r2 = new LinkedHashMap<String, Object>();
			for(String key: result.keySet()) {
				r2.put(String.format(captionFormat, key), result.get(key));				
			}
			return r2;
		}
		else {
			return result;
		}
	}

	private class DecoReader implements SampleReader {
		
		private final SampleReader reader;

		public DecoReader(SampleReader reader) {
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
			List<Object> list = new ArrayList<Object>(reader.keySet());
			filter(list);
			return list;
		}

		private void filter(List<Object> list) {
			Iterator<Object> it = list.iterator();
			while(it.hasNext()) {
				Object next = it.next();
				if (next instanceof Decorated && ((Decorated)next).startsWith(deco)) {
					continue;					
				}
				it.remove();
			}			
		}

		@Override
		public Object get(Object key) {
			return reader.get(decorate(key));
		}

		private Object decorate(Object key) {
			if (key instanceof Decorated) {
				List<Object> nd  = new ArrayList<Object>();
				nd.addAll(deco);
				nd.addAll(((Decorated) key).getDecoration());
				return new Decorated(nd, key);				
			}
			else {
				return new Decorated(deco, key);				
			}
		}
	}	
}

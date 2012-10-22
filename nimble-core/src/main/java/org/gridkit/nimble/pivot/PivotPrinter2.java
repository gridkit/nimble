package org.gridkit.nimble.pivot;

import java.util.ArrayList;
import java.util.List;

import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.print.LinePrinter;

public class PivotPrinter2 {
	
	private final List<DisplayFunction> functions = new ArrayList<DisplayFunction>();
	private boolean dumpUnprinted;
	
	public void add(DisplayFunction function) {
		this.functions.add(function);
	}
	
	public void dumpUnprinted() {
		dumpUnprinted = true;
	}

	
//	public LinePrinter print(SampleReader reader) {
//		
//	}
	
	private static class DisplayFunction {
		
	}
	
	private static class SingleSampleReader implements SampleReader {

		final List<Object> touchedKeys = new ArrayList<Object>();
		final SampleReader reader;
		
		public SingleSampleReader(SampleReader reader) {
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
			return reader.keySet();
		}

		@Override
		public Object get(Object key) {
			if (!touchedKeys.contains(key)) {
				touchedKeys.add(key);
			}
			return reader.get(key);
		}
	}
}

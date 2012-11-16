package org.gridkit.nimble.pivot.display;

import java.util.ArrayList;
import java.util.List;

import org.gridkit.nimble.pivot.SampleExtractor;

public interface PrintConfig {

	public abstract void add(String pattern, DisplayComponent component);

	public abstract void add(DisplayComponent component);

	public abstract void sortByColumn(String... colName);

	public abstract void sortBy(SampleExtractor extractor);

	public abstract void sortByField(Object... key);

	public static class Recorder implements PrintConfig {
		
		private interface Action {
			void perform(PrintConfig config);
		}
		
		private List<Action> actions = new ArrayList<Action>();

		public void replay(PrintConfig config) {
			for(Action a: actions) {
				a.perform(config);
			}
		}
		
		@Override
		public void add(final String pattern, final DisplayComponent component) {
			actions.add(new Action() {
				@Override
				public void perform(PrintConfig config) {
					config.add(pattern, component);					
				}
			});
		}

		@Override
		public void add(final DisplayComponent component) {
			actions.add(new Action() {
				@Override
				public void perform(PrintConfig config) {
					config.add(component);					
				}
			});
		}

		@Override
		public void sortByColumn(final String... colName) {
			actions.add(new Action() {
				@Override
				public void perform(PrintConfig config) {
					config.sortByColumn(colName);					
				}
			});
		}

		@Override
		public void sortBy(final SampleExtractor extractor) {
			actions.add(new Action() {
				@Override
				public void perform(PrintConfig config) {
					config.sortBy(extractor);					
				}
			});
		}

		@Override
		public void sortByField(final Object... keys) {
			actions.add(new Action() {
				@Override
				public void perform(PrintConfig config) {
					config.sortByField(keys);					
				}
			});
		}
	}	
}

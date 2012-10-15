package org.gridkit.nimble.orchestration;

import org.gridkit.nimble.orchestration.ScenarioBuilder;
import org.junit.Test;

public class ScenarioBuilderTest {

	@Test
	public void test_scenario() {
		
		ScenarioBuilder sb = new ScenarioBuilder();
		
		sb.start();
		
		sb.natural();
		
		Agent a1 = sb.deploy(new SimpleAgent("AGENT1"));
		Agent a2 = sb.deploy(new SimpleAgent("AGENT2"));
		
		a1.connect();
		a2.connect();
		
		Reporter r1 = sb.deploy(new SimpleReporter("R1"));
		Reporter r2 = sb.deploy(new SimpleReporter("R2"));
		
//		sb.sync();
		
		a1.report(r1);
		a2.report(r1);
		
//		sb.sync();
		
		a1.report(r2);
		a2.report(r2);
		
		sb.finish();
		
		sb.getScenario().play(null);
		
	}
	
	
	public static interface Agent {
		
		public void connect();
		
		public void report(Reporter reporter);
		
	}
	
	public static interface Reporter {
		
		
	}
	
	public static class SimpleAgent implements Agent {

		private String name;
		
		public SimpleAgent(String name) {
			this.name = name;
		}

		@Override
		public void connect() {
			System.out.println("[" + name + "] connect");
		}

		@Override
		public void report(Reporter reporter) {
			System.out.println("[" + name + "] report(" + reporter + ")");
		}
		
		@Override
		public String toString() {
			return "Agent [" + name + "]";
		}
	}
	
	public static class SimpleReporter implements Reporter {
		
		private final String text;

		public SimpleReporter(String text) {
			this.text = text;
		}

		@Override
		public String toString() {
			return text;
		}
	}
}

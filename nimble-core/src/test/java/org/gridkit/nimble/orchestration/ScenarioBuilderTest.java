package org.gridkit.nimble.orchestration;

import org.gridkit.nimble.orchestration.ScenarioBuilder;
import org.gridkit.nimble.orchestration.ScenarioBuilderTest.Reporter;
import org.junit.Test;

public class ScenarioBuilderTest {

	@Test
	public void test_scenario() {
		
		ScenarioBuilder sb = new ScenarioBuilder();
		
		sb.natural();
		
		Agent a1 = sb.deploy(new SimpleAgent("AGENT1"));
		Agent a2 = sb.deploy(new SimpleAgent("AGENT2"));
		
		a1.connect();
		a2.connect();
		
		Reporter r1 = sb.deploy(new SimpleReporter("R1"));
		Reporter r2 = sb.deploy(new SimpleReporter("R2"));
		
		sb.checkpoint("run");
		
		a1.report(r1);
		a2.report(r1);
		
		sb.sync();
		
		a1.report(r2);
		a2.report(r2);
		
		sb.checkpoint("done");
		
		sb.fromStart();
		
		ReportingSupport rs = sb.deploy(new SimpleReportingSupport());
		
		rs.addReporter(r1);
		rs.addReporter(r2);
		
		sb.join("run");
		
		sb.from("done");
		
		rs.collect();
		
		sb.getScenario().play(null);
		
	}
	
	
	public static interface Agent {
		
		public void connect();
		
		public void report(Reporter reporter);
		
	}
	
	public static interface Reporter {
		
		
	}
	
	public static interface ReportingSupport {
		
		public void addReporter(Reporter rep);
		
		public void collect();
		
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

	public static class SimpleReportingSupport implements ReportingSupport {
		
		public SimpleReportingSupport() {
		}
		
		@Override
		public void addReporter(Reporter rep) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void collect() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public String toString() {
			return getClass().getSimpleName();
		}
	}
}

package org.gridkit.nimble.orchestration;

public class TimeLine {

	private final String init;
	private final String start;
	private final String stop;
	private final String done;

	public TimeLine(String init, String start, String stop, String done) {
		this.init = init;
		this.start = start;
		this.stop = stop;
		this.done = done;
	}

	public String getInitCheckpoint() {
		return init;
	}

	public String getStartCheckpoint() {
		return start;
	}

	public String getStopCheckpoint() {
		return stop;
	}

	public String getDoneCheckpoint() {
		return done;
	}
}

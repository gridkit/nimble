package org.gridkit.lab.monitoring.probe;

public interface PollProbe {

	public void poll();
	
	public void stop();
	
	public static class BrokenProbeTargetException extends RuntimeException {

		private static final long serialVersionUID = 20121106L;

		public BrokenProbeTargetException() {
			super();
		}

		public BrokenProbeTargetException(String message, Throwable cause) {
			super(message, cause);
		}

		public BrokenProbeTargetException(String message) {
			super(message);
		}

		public BrokenProbeTargetException(Throwable cause) {
			super(cause);
		}
	}
}

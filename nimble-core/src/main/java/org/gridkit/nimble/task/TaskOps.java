package org.gridkit.nimble.task;

import org.gridkit.nimble.platform.RemoteAgent;
import org.gridkit.nimble.scenario.Scenario;
import org.slf4j.Logger;

public class TaskOps {
    public static void logNoTasksFound(Logger log, Scenario scenario) {
        log.info("No tasks was found for scenario '{}'", scenario);
    }
    
    public static void logNoAgentsFound(Logger log, Scenario scenario) {
        log.info("No agents was found for scenario '{}'", scenario);
    }
    
    public static void logNoTaskToExecute(Logger log, Scenario scenario, RemoteAgent agent) {
        log.info("Agent '{}' has no tasks to execute in scenario '{}'", agent, scenario);
    }
    
    public static String getExecName(Scenario scenario, RemoteAgent agent) {
        return scenario.toString() + "#" + agent.toString();
    }
}

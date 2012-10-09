package org.gridkit.nimble.scenario;

import static org.gridkit.nimble.util.StringOps.F;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.gridkit.nimble.platform.Play;
import org.slf4j.Logger;

public class ScenarioOps {
    public static String getCompositeName(String type, Collection<Scenario> scenarios) {
        Set<String> names = new LinkedHashSet<String>();
        
        for (Scenario scenario : scenarios) {
            names.add(scenario.toString());
        }
        
        return getName(type, names);
    }
    
    public static String getName(String type, Collection<String> elements) {
        return type + (new ArrayList<String>(elements)).toString();
    }
    
    public static String getName(String type, String... elements) {
        return type + (Arrays.asList(elements)).toString();
    }
    
    public static void logStart(Logger log, Scenario scenario) {
        log.info("Starting execution of Scenario '{}'", scenario);
    }

    public static void logSuccess(Logger log, Scenario scenario) {
        log.info("Scenario '{}' finished succesfully", scenario);
    }
    
    public static void logCancel(Logger log, Scenario scenario) {
        log.info("Scenario '{}' was canceled", scenario);
    }
    
    public static void logFailure(Logger log, Scenario scenario, Throwable t) {
        if (log.isWarnEnabled()) {
            if (t != null) {
                log.warn(F("Scenario '%s' was failed due to exception", scenario), t);
            } else {
                log.warn(F("Scenario '%s' was failed. Exception is missed", scenario), t);
            }
        }
    }
    
    public static void logFailure(Logger log, Scenario scenario, String cause) {
        log.warn("Scenario '{}' was failed due to fail of '{}'", scenario, cause);
    }
    
    public static void logFailure(Logger log, Scenario scenario, String cause, Play.Status status) {
        if (log.isErrorEnabled()) {
            log.error(F(
                "Scenario '%s' got incorrect status '%s' from scenario '{}' and failed",
                scenario, status, cause
            ));
        }
    }
    
    public static void logFailure(Logger log, Scenario master, Scenario worker) {
        logFailure(log, master, F("scenarion '%s'", worker));
    }
    
    public static void logFailure(Logger log, Scenario master, Scenario worker, Play.Status status) {
        logFailure(log, master, F("scenarion '%s'", worker), status);
    }
}

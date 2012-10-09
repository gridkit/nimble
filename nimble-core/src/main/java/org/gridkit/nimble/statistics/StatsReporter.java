package org.gridkit.nimble.statistics;

import java.util.Map;

public interface StatsReporter {
    void report(Map<String, Object> sample);
}

package org.gridkit.nimble.task;

import java.io.Serializable;

import org.gridkit.nimble.statistics.StatsReporter;

@SuppressWarnings("serial")
public class SingletonStatsReporterFactory<R extends StatsReporter> implements TaskScenario.StatsReporterFactory<R>, Serializable {
    private R reporter;

    public SingletonStatsReporterFactory(R reporter) {
        this.reporter = reporter;
    }

    @Override
    public R newTaskReporter() {
        return reporter;
    }

    @Override
    public void finish(R taskRep) {
        
    }

    @Override
    public void finish() {
        
    }
}

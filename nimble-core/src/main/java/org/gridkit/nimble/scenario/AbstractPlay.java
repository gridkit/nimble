package org.gridkit.nimble.scenario;

import org.gridkit.nimble.platform.Play;

public abstract class AbstractPlay implements Play {
    protected final Scenario scenario;

    private Play.Status status;
    
    public AbstractPlay(Scenario scenario) {
        this.scenario = scenario;
        this.status = Play.Status.InProgress;
    }

    @Override
    public Scenario getScenario() {
        return scenario;
    }

    @Override
    public synchronized Play.Status getStatus() {
        return status;
    }
    
    protected synchronized void update(Runnable runnable) {
        runnable.run();
    }

    protected synchronized boolean setStatus(Play.Status status) {
        if (this.status == Play.Status.InProgress) {
            this.status = status;
            return true;
        }
        return false;
    }
}

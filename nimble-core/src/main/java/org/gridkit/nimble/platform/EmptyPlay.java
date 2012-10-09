package org.gridkit.nimble.platform;

import org.gridkit.nimble.scenario.Scenario;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class EmptyPlay implements Play {
    private final Scenario scenario;
    private final Play.Status status;
    private final ListenableFuture<Void> future;
    
    public EmptyPlay(Scenario scenario, Play.Status status) {
        this.scenario = scenario;
        this.status = status;
        this.future = Futures.immediateFuture(null);
    }

    public EmptyPlay(Scenario scenario) {
        this(scenario, Play.Status.Success);
    }
    
    @Override
    public Scenario getScenario() {
        return scenario;
    }

    @Override
    public Play.Status getStatus() {
        return status;
    }

    @Override
    public ListenableFuture<Void> getCompletionFuture() {
        return future;
    }
}

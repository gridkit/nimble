package org.gridkit.nimble.scenario;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.gridkit.nimble.platform.Play;
import org.gridkit.nimble.platform.RemoteAgent;
import org.gridkit.nimble.util.SetOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemonScenario implements Scenario {
    private static final Logger log = LoggerFactory.getLogger(DemonScenario.class);
    
    private String name;
    private Scenario scenario;
    private Set<String> labels;
    private Collection<RemoteAgent.Invocable<Void>> demons;
    
    public DemonScenario(String name, Scenario scenario, Set<String> labels, Collection<RemoteAgent.Invocable<Void>> demons) {
        this.name = name;
        this.scenario = scenario;
        this.labels = labels;
        this.demons = demons;
    }

    public static DemonScenario newInstance(String name, Scenario scenario, Set<String> labels, Collection<Callable<Void>> demons) {
        return new DemonScenario(name, scenario, labels, convert(demons));
    }
    
    private static Collection<RemoteAgent.Invocable<Void>> convert(Collection<Callable<Void>> demons) {
        Collection<RemoteAgent.Invocable<Void>> result = new ArrayList<RemoteAgent.Invocable<Void>>();
        
        for (Callable<Void> demon : demons) {
            result.add(new RemoteAgent.CallableInvocable<Void>(demon));
        }
        
        return result;
    }
    
    @Override
    public Play play(Context context) {
        ScenarioOps.logStart(log, this);
        
        List<Future<Void>> demonFutures = startDemons(context.getAgents());
        
        Play scenPlay = scenario.play(context);
        
        scenPlay.getCompletionFuture().addListener(new DemonHalt(demonFutures), context.getExecutor());
        
        return scenPlay;
    }
    
    private List<Future<Void>> startDemons(Collection<RemoteAgent> agents) {
        List<Future<Void>> result = new ArrayList<Future<Void>>();
        
        for (RemoteAgent agent : filterAgents(agents)) {
            for (RemoteAgent.Invocable<Void> invocable : demons) {
                result.add(agent.invoke(invocable));
            }
        }
        
        return result;
    }
    
    private Collection<RemoteAgent> filterAgents(Collection<RemoteAgent> agents) {
        if (labels == null) {
            return agents;
        }
        
        List<RemoteAgent> result = new ArrayList<RemoteAgent>();
        
        for (RemoteAgent agent : agents) {
            Set<String> inter = SetOps.intersection(agent.getLabels(), labels);
            
            if (inter.size() > 0) {
                result.add(agent);
            }
        }
        
        return result;
    }

    private class DemonHalt implements Runnable {
        private final List<Future<Void>> demonFutures;

        public DemonHalt(List<Future<Void>> demonFutures) {
            this.demonFutures = demonFutures;
        }

        @Override
        public void run() {
            for (Future<Void> demonFuture : demonFutures) {
                demonFuture.cancel(true);
            }
            ScenarioOps.logSuccess(log, DemonScenario.this);
        }
    }
    
    @Override
    public String toString() {
        return ScenarioOps.getName("Demon", name, scenario.toString());
    }
}

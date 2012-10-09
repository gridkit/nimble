package org.gridkit.nimble.scenario;

import static org.gridkit.nimble.util.StringOps.F;

import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.gridkit.nimble.platform.LocalAgent;
import org.gridkit.nimble.platform.Play;
import org.gridkit.nimble.platform.RemoteAgent;
import org.gridkit.nimble.util.FutureListener;
import org.gridkit.nimble.util.FutureOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;

public class ExecScenario implements Scenario {
    private static final Logger log = LoggerFactory.getLogger(ExecScenario.class);
    
    public static interface Context {       
        LocalAgent getLocalAgent();
        
        ConcurrentMap<String, Object> getAttrsMap();
    }
    
    public static interface Executable extends Serializable {
        public Play.Status excute(Context context) throws Exception;
    }

    private final Executable executable;
    
    private final RemoteAgent agent;
        
    public ExecScenario(Executable executable, RemoteAgent agent) {
        this.executable = executable;
        this.agent = agent;
    }
    
    @Override
    public Play play(Scenario.Context context) {
        ExecPlay play = new ExecPlay(this, context, agent);
        play.action();
        return play;
    }

    private class ExecPlay extends AbstractPlay {
        private final ExecPipeline pipeline;
        
        public ExecPlay(Scenario scenario, Scenario.Context context, RemoteAgent agent) {
            super(scenario);
            pipeline = new ExecPipeline(context);
        }
        
        public void action() {
            pipeline.start(this);
        }
        
        @Override
        public ListenableFuture<Void> getCompletionFuture() {
            return pipeline;
        }
    }
    
    private class ExecPipeline extends AbstractFuture<Void> implements FutureListener<Play.Status> {
        private final Scenario.Context context;
        
        private volatile AbstractPlay play;

        private volatile ListenableFuture<Play.Status> future;
        
        public ExecPipeline(Scenario.Context context) {
            this.context = context;
        }
        
        public void start(AbstractPlay play) {
            ScenarioOps.logStart(log, ExecScenario.this);
            
            this.play = play;
            
            Executor executor = new Executor(context.getContextId(), executable);
            
            future = agent.invoke(executor);

            FutureOps.addListener(future, this, context.getExecutor());
        }

        @Override
        public void onSuccess(final Play.Status result) {
            play.update(new Runnable() {
                @Override
                public void run() {
                    if (result == Play.Status.Failure) {
                        play.setStatus(Play.Status.Failure);
                        ScenarioOps.logFailure(log, ExecScenario.this, executable.toString());
                    } else if (result == Play.Status.Success) {
                        play.setStatus(Play.Status.Success);
                        ScenarioOps.logSuccess(log, ExecScenario.this);
                    } else {
                        play.setStatus(Play.Status.Failure);
                        ScenarioOps.logFailure(log, ExecScenario.this, executable.toString());
                    }
                    
                    set(null);
                }
            });
        }

        @Override
        public void onFailure(Throwable t, FailureEvent event) {
            if (play.setStatus(Play.Status.Failure)) {
                ScenarioOps.logFailure(log, ExecScenario.this, t);
                setException(t);
            }
        }
        
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (isDone()) {
                return false;
            }
            
            try {
                future.cancel(mayInterruptIfRunning);
            } finally {
                if (play.setStatus(Play.Status.Canceled)) {
                    ScenarioOps.logCancel(log, ExecScenario.this);
                }
            }

            return super.cancel(false);
        }

        @Override
        public void onCancel() {
            
        }
    }

    @SuppressWarnings("serial")
    private static class Executor implements RemoteAgent.Invocable<Play.Status>, Context {
        private String contextId;
        private Executable executable;
        
        private transient LocalAgent agent;
        
        public Executor(String contextId, Executable executable) {
            this.contextId = contextId;
            this.executable = executable;
        }

        @Override
        public Play.Status invoke(LocalAgent agent) {
            this.agent = agent;
            
            try {
                return executable.excute(this);
            } catch (Throwable t) {                
                agent.getLogger(Executor.class.getName()).error(
                    F("Exception during executable '%s' execution on agent '%s'", executable.toString()), t
                );
                
                return Play.Status.Failure;
            }
        }
        
        @Override
        public LocalAgent getLocalAgent() {
            return agent;
        }
        
        @Override //TODO implement attributes cleaning
        public ConcurrentMap<String, Object> getAttrsMap() {
            if (!agent.getAttrsMap().containsKey(contextId)) {
                agent.getAttrsMap().putIfAbsent(contextId, new ConcurrentHashMap<String, Object>());
            }

            @SuppressWarnings("unchecked")
            ConcurrentMap<String, Object> result = (ConcurrentMap<String, Object>)agent.getAttrsMap().get(contextId);
            
            return result;
        }
    }
    
    @Override
    public String toString() {
        return ScenarioOps.getName("Exec", Arrays.asList(executable.toString(), agent.toString()));
    }
}

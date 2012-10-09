package org.gridkit.nimble.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.gridkit.nimble.platform.EmptyPlay;
import org.gridkit.nimble.platform.Play;
import org.gridkit.nimble.platform.RemoteAgent;
import org.gridkit.nimble.scenario.ExecScenario;
import org.gridkit.nimble.scenario.ParScenario;
import org.gridkit.nimble.scenario.Scenario;
import org.gridkit.nimble.scenario.ScenarioOps;
import org.gridkit.nimble.statistics.StatsReporter;
import org.gridkit.nimble.statistics.simple.SimpleStatsAggregator;
import org.gridkit.nimble.util.FutureListener;
import org.gridkit.nimble.util.FutureOps;
import org.gridkit.nimble.util.ValidOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskScenario implements Scenario, FutureListener<Void> {
    private static final Logger log = LoggerFactory.getLogger(TaskScenario.class);
    
    private String name;
    private List<Task> tasks;
    private TaskSLA sla;
    private StatsReporterFactory<?> repFactory;
        
    public static interface StatsReporterFactory<R extends StatsReporter> {
        R newTaskReporter();
        
        void finish(R taskRep);
        
        void finish();
    }
    
    public TaskScenario(String name, Collection<Task> tasks, TaskSLA sla, StatsReporterFactory<?> repFactory) {
        ValidOps.notEmpty(name, "name");
        ValidOps.notNull(tasks, "tasks");
        ValidOps.notNull(sla, "sla");
        ValidOps.notNull(repFactory, "repFactory");
        
        this.name = name;
        this.tasks = new ArrayList<Task>(tasks);
        this.sla = sla;
        this.repFactory = repFactory;
        
        this.sla.shuffle(this.tasks);
    }
    
    public TaskScenario(String name, Collection<Task> tasks, TaskSLA sla, SimpleStatsAggregator aggr) {
        this(name, tasks, sla, new SimpleStatsReporterFactory(aggr));
    }
    
    public <R extends StatsReporter> TaskScenario(String name, Collection<Task> tasks, TaskSLA sla, R reporter) {
        this(name, tasks, sla, new SingletonStatsReporterFactory<R>(reporter));
    }

    @Override
    public Play play(Context context) {
        ScenarioOps.logStart(log, this);
        
        List<RemoteAgent> agents = sla.getAgents(context.getAgents());
                
        Play result;
        
        if (agents.isEmpty()) {
            TaskOps.logNoAgentsFound(log, this);
            result = new EmptyPlay(this);
        } else if (tasks.isEmpty()) {
            TaskOps.logNoTasksFound(log, this);
            result = new EmptyPlay(this);
        } else if (agents.size() == 1) {
            RemoteAgent execAgent = agents.get(0);
            result = newAgentScenario(execAgent, tasks, TaskOps.getExecName(this, execAgent)).play(context);
        } else {
            List<Scenario> parScenarios = new ArrayList<Scenario>(agents.size());
            
            List<List<Task>> distributedTasks = sla.getDistribution().distribute(tasks, agents.size());
            
            for (int i = 0; i < agents.size(); ++i) {
                List<Task> execTasks = distributedTasks.get(i);
                RemoteAgent execAgent = agents.get(i);
                
                if (!execTasks.isEmpty()) {
                    parScenarios.add(newAgentScenario(execAgent, execTasks, TaskOps.getExecName(this, execAgent)));
                } else {
                    TaskOps.logNoTaskToExecute(log, this, execAgent);
                }
            }
            
            result = (new ParScenario(parScenarios)).play(context);
        }

        FutureOps.addListener(result.getCompletionFuture(), this, context.getExecutor());
        
        return result;
    }
    
    private Scenario newAgentScenario(RemoteAgent agent, List<Task> tasks, String name) {
        TaskExecutable executable = new TaskExecutable(name, tasks, sla, repFactory);
        Scenario scenario = new ExecScenario(executable, agent);
        return scenario;        
    }

    @Override
    public String toString() {
        return ScenarioOps.getName("Task", name);
    }

    @Override
    public void onSuccess(Void result) {
        //TODO fix, failures also go here
        ScenarioOps.logSuccess(log, this);
    }

    @Override
    public void onFailure(Throwable t, FailureEvent event) {
        ScenarioOps.logFailure(log, this, t);
    }

    @Override
    public void onCancel() {
        ScenarioOps.logCancel(log, this);
    }
}

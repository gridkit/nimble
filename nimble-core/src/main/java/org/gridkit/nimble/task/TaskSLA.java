package org.gridkit.nimble.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.gridkit.nimble.platform.Play;
import org.gridkit.nimble.platform.RemoteAgent;
import org.gridkit.nimble.util.NamedThreadFactory;
import org.gridkit.nimble.util.ValidOps;
import org.gridkit.util.concurrent.Barriers;
import org.gridkit.util.concurrent.BlockingBarrier;

@SuppressWarnings("serial")
public class TaskSLA implements Cloneable, Serializable {
    private Distribution distribution = Distribution.All;
    
    private Integer threadsCount = null; // by default thread per task
    
    private Long startDelay = null; // by default start immediately
    
    private Long finishDelay = null;
    
    private Double rate = null;
    
    private Long iterationsCount = 1l; 

    private boolean ignoreFailures = false;
    
    private boolean shuffle = true;
    
    private Set<String> labels = null; // by default execute on every agent
    
    public static enum Distribution {
        // all tasks on all agents
        All { 
            @Override
            public List<List<Task>> distribute(List<Task> tasks, int agentsCount) {
                List<List<Task>> result = new ArrayList<List<Task>>(agentsCount);
                
                for (int i = 0; i < agentsCount; ++i) {
                    result.add(tasks);
                }

                return result;
            }
        },
        // spread task between available agents
        Spread {
            @Override
            public List<List<Task>> distribute(List<Task> tasks, int agentsCount) {
                List<List<Task>> result = new ArrayList<List<Task>>(agentsCount);
                
                for (int i = 0; i < agentsCount; ++i){
                    result.add(new ArrayList<Task>());
                }
                
                for (int i = 0; i < tasks.size(); ++i) {
                    result.get(i % agentsCount).add(tasks.get(i));
                }
                
                return result;
            }
        }; 
        
        public abstract List<List<Task>> distribute(List<Task> tasks, int agentsCount);
    }
    
    public ExecutorService newExecutor(String taskName) {
        ThreadFactory threadFactory = new NamedThreadFactory(taskName);
        
        if (threadsCount == null) {
            return Executors.newCachedThreadPool(threadFactory);
        } else {
            return Executors.newFixedThreadPool(threadsCount, threadFactory);
        }
    }
    
    public List<RemoteAgent> getAgents(Collection<RemoteAgent> agents) {
        List<RemoteAgent> result = new ArrayList<RemoteAgent>();
        
        for (RemoteAgent agent : agents) {
            if (isLabeled(agent)) {
                result.add(agent);
            }
        }
        
        return result;
    }
    
    private boolean isLabeled(RemoteAgent agent) {
        if (labels == null) { 
            return true;
        } else {
            for (String label : labels) {
                if (agent.getLabels().contains(label)) {
                    return true;
                }
            }
            return false;
        }
    }
    
    public List<Task> shuffle(List<Task> tasks) {
        if (shuffle) {
            Collections.shuffle(tasks);
        };
        
        return tasks;
    }
    
    public Play.Status getStatus(Play.Status status) {
        return ignoreFailures ? Play.Status.Success : status;
    }
    
    public void waitForStart() throws InterruptedException {
        if (startDelay != null) {
            Thread.sleep(startDelay);
        }
    }
    
    public boolean isFinished(long duration, long iteration) {        
        if (finishDelay != null && duration > finishDelay) {
            return true;
        }
        
        if (iterationsCount != null && iteration >= iterationsCount) {
            return true;
        }

        return false;
    }

    public Distribution getDistribution() {
        return distribution;
    }

    public void setDistribution(Distribution distribution) {
        ValidOps.notNull(distribution, "distribution");
        
        this.distribution = distribution;
    }

    public Integer getThreadsCount() {
        return threadsCount;
    }

    public void setThreadsCount(Integer threadsCount) {
        if (threadsCount != null) {
            ValidOps.positive(threadsCount, "threadsCount");
        }
        
        this.threadsCount = threadsCount;
    }

    public Long getStartDelay() {
        return startDelay;
    }

    public void setStartDelay(Long startDelay, TimeUnit timeUnit) {
        if (startDelay != null) {
            ValidOps.positive(startDelay, "startDelay");
        }
        
        this.startDelay = TimeUnit.MILLISECONDS.convert(startDelay, timeUnit);
    }

    public Long getFinishDelay() {
        return finishDelay;
    }

    public void setFinishDelay(Long finishDelay, TimeUnit timeUnit) {
        if (finishDelay != null) {
            ValidOps.positive(finishDelay, "finishDelay");
        }
        
        this.finishDelay = TimeUnit.MILLISECONDS.convert(finishDelay, timeUnit);
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        if (rate != null) {
            ValidOps.positive(rate, "rate");
        }
        
        this.rate = rate;
    }

    public BlockingBarrier getTaskBarrier() {
        return Barriers.speedLimit(rate == null ? Double.POSITIVE_INFINITY : rate);
    }
    
    public Long getIterationsCount() {
        return iterationsCount;
    }

    public void setIterationsCount(Long iterationsCount) {
        if (iterationsCount != null) {
            ValidOps.positive(iterationsCount, "iterationsCount");
        }
        
        this.iterationsCount = iterationsCount;
    }

    public boolean isIgnoreFailures() {
        return ignoreFailures;
    }

    public void setIgnoreFailures(boolean ignoreFailures) {
        this.ignoreFailures = ignoreFailures;
    }

    public Set<String> getLabels() {
        return labels;
    }

    public void setLabels(Set<String> labels) {
        if (labels != null) {
            ValidOps.notEmpty(labels, "labels");
        }

        this.labels = labels;
    }

    public boolean isShuffle() {
        return shuffle;
    }

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }

    @Override
    public TaskSLA clone() {
        TaskSLA result = new TaskSLA();
        
        result.distribution = this.distribution;
        result.threadsCount = this.threadsCount;
        result.startDelay = this.startDelay;
        result.finishDelay = this.finishDelay;
        result.rate = this.rate;
        result.iterationsCount = this.iterationsCount; 
        result.labels = new HashSet<String>(labels);
        
        return result;
    }
}

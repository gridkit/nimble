package org.gridkit.nimble.task;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.gridkit.nimble.statistics.simple.QueuedSimpleStatsAggregator;
import org.gridkit.nimble.statistics.simple.SimpleStatsAggregator;
import org.gridkit.nimble.statistics.simple.SimpleStatsProducer;

@SuppressWarnings("serial")
public class SimpleStatsReporterFactory implements TaskScenario.StatsReporterFactory<SimpleStatsProducer>, Serializable {
    private SimpleStatsAggregator globalAggregator;
    
    private transient SimpleStatsAggregator taskAggregator;
    private transient Set<SimpleStatsProducer> taskProducers;
    
    public SimpleStatsReporterFactory(SimpleStatsAggregator globalAggregator) {
        this.globalAggregator = globalAggregator;
        this.init();
    }

    @Override
    public SimpleStatsProducer newTaskReporter() {
        SimpleStatsProducer result = new SimpleStatsProducer();
        taskProducers.add(result);
        return result;
    }
    
    
    @Override
    public void finish(SimpleStatsProducer taskRep) {
        taskAggregator.add(taskRep.produce());
        taskProducers.remove(taskRep);
    }

    @Override
    public void finish() {
        globalAggregator.add(taskAggregator.calculate());
    }
    
    private void init() {
        this.taskAggregator = new QueuedSimpleStatsAggregator();
        this.taskProducers = Collections.synchronizedSet(Collections.newSetFromMap(
            new IdentityHashMap<SimpleStatsProducer, Boolean>()
        ));
    }
    
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.init();
    }
}

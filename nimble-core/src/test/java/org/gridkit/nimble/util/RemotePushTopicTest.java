package org.gridkit.nimble.util;

import java.rmi.Remote;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.gridkit.nimble.platform.Director;
import org.gridkit.nimble.platform.Play;
import org.gridkit.nimble.platform.PushTopic;
import org.gridkit.nimble.platform.RemoteAgent;
import org.gridkit.nimble.platform.remote.LocalAgentFactory;
import org.gridkit.nimble.scenario.Scenario;
import org.gridkit.nimble.scenario.SeqScenario;
import org.gridkit.nimble.statistics.simple.QueuedSimpleStatsAggregator;
import org.gridkit.nimble.statistics.simple.SimpleStatsAggregator;
import org.gridkit.nimble.task.SimpleStatsReporterFactory;
import org.gridkit.nimble.task.Task;
import org.gridkit.nimble.task.TaskSLA;
import org.gridkit.nimble.task.TaskScenario;
import org.junit.Test;

public class RemotePushTopicTest {
    public static final String SUBSCRIBER = "SUBSCRIBER";
    public static final String PUBLISHER = "PUBLISHER";
    
    @Test
    public void main() throws Exception {
        PushTopic<String> topic = new RemotePushTopic<String>();
        
        TaskSLA initSla = new TaskSLA();
        initSla.setLabels(Collections.singleton(SUBSCRIBER));
        Task initTask = new InitSubscriber(topic);
        
        TaskSLA publishSla = new TaskSLA();
        publishSla.setLabels(Collections.singleton(PUBLISHER));
        Task publishTask = new PublishTask(topic, 1, 10);
        
        SimpleStatsAggregator aggr = new QueuedSimpleStatsAggregator();    
        
        Scenario scenario = new SeqScenario(Arrays.<Scenario>asList(
            new TaskScenario("INIT", Collections.singleton(initTask), initSla, new SimpleStatsReporterFactory(aggr)),
            new TaskScenario("PUBLISH", Collections.singleton(publishTask), publishSla, new SimpleStatsReporterFactory(aggr))
        ));
        
        LocalAgentFactory localFactory = new LocalAgentFactory();
        
        RemoteAgent publisher   = localFactory.createAgent("publisher", PUBLISHER);
        RemoteAgent subscriber1 = localFactory.createAgent("subscriber1", SUBSCRIBER);
        RemoteAgent subscriber2 = localFactory.createAgent("subscriber2", SUBSCRIBER);
                
        Director director = new Director(
            Arrays.asList(publisher, subscriber1, subscriber2)
        );

        try {
            Play play = director.play(scenario);
            play.getCompletionFuture().get();
            topic.sync();
        } finally {
            director.shutdown(false);
        }
    }
    
    @SuppressWarnings("serial")
    public static class InitSubscriber implements Task {
        private final PushTopic<String> topic;
        
        public InitSubscriber(PushTopic<String> topic) {
            this.topic = topic;
        }

        @Override
        public void excute(Context context) throws Exception {
            topic.subscribe(new PrintSubscriber());
        }
    }

    public static class PrintSubscriber implements PushTopic.Subscriber<String>, Remote {
        @Override
        public void push(Collection<String> msgs) {
        	System.out.println(msgs);
//            for (String msg : msgs) {
//            }
        }
    }
    
    @SuppressWarnings("serial")
    public static class PublishTask implements Task {
        private final PushTopic<String> topic;
        
        private final int from;
        private final int to;
        
        public PublishTask(PushTopic<String> topic, int from, int to) {
            this.topic = topic;
            this.from = from;
            this.to = to;
        }

        @Override
        public void excute(Context context) throws Exception {
        	try {
	            for (int i = from; i < to; ++i) {
	                topic.publish(Collections.singleton(String.valueOf(i)));
	            }
	            topic.sync();
	            List<String> pack = new ArrayList<String>();
	            for (int i = 0; i != 16 << 10; ++i) {
	            	pack.add(String.valueOf(i));
	            }
	            topic.publish(pack);
	            topic.sync();
        	}
        	catch(Exception e) {
        		e.printStackTrace();
        	}
        }
    }
}

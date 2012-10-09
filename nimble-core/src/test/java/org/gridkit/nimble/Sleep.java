package org.gridkit.nimble;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CancellationException;

import org.gridkit.nimble.platform.Director;
import org.gridkit.nimble.platform.Play;
import org.gridkit.nimble.platform.RemoteAgent;
import org.gridkit.nimble.platform.local.ThreadPoolAgent;
import org.gridkit.nimble.platform.remote.LocalAgentFactory;
import org.gridkit.nimble.scenario.ExecScenario;
import org.gridkit.nimble.scenario.ExecScenario.Context;
import org.gridkit.nimble.scenario.ExecScenario.Executable;
import org.gridkit.nimble.scenario.ParScenario;
import org.gridkit.nimble.scenario.Scenario;
import org.gridkit.nimble.scenario.SeqScenario;
import org.junit.Test;
import org.slf4j.Logger;

public class Sleep {
    @Test(expected = CancellationException.class)
    public void testInproc() throws Exception {
        RemoteAgent agent = new ThreadPoolAgent();
                
        test(agent);
    }
    
    @Test(expected = CancellationException.class)
    public void testLocal() throws Exception {
        LocalAgentFactory localFactory = new LocalAgentFactory();
        
        RemoteAgent agent = localFactory.createAgent("agent");
        
        test(agent);
    }
    
    public static void test(RemoteAgent agent) throws Exception {
        Director director = new Director(Collections.singletonList(agent));
        
        Scenario s1 = new ExecScenario(new SimpleExecutable("A"), agent);
        Scenario s2 = new ExecScenario(new SimpleExecutable("B"), agent);
        Scenario s3 = new ExecScenario(new SimpleExecutable("C"), agent);
        
        Scenario s4 = new ExecScenario(new SimpleExecutable("D"), agent);
        Scenario s5 = new ExecScenario(new SimpleExecutable("E"), agent);
        Scenario s6 = new ExecScenario(new SimpleExecutable("F"), agent);
        
        Scenario seq1 = new SeqScenario(Arrays.asList(s1, s2, s3));
        Scenario seq2 = new SeqScenario(Arrays.asList(s4, s5, s6));

        Scenario par = new ParScenario(Arrays.asList(seq1, seq2));
        
        Play play = director.play(par);
        
        Thread.sleep(1250);
        
        System.out.println(play.getStatus());
        
        play.getCompletionFuture().cancel(true);
        
        try {
            play.getCompletionFuture().get();
        } finally {
            Thread.sleep(100);
            System.out.println(play.getStatus());
            director.shutdown(true);
        }
    }
    
    @SuppressWarnings("serial")
    public static class SimpleExecutable implements Executable {
        private final String str;

        public SimpleExecutable(String str) {
            this.str = str;
        }

        @Override
        public Play.Status excute(Context context) throws Exception {
            Logger log = context.getLocalAgent().getLogger(str);
            
            log.info(str + " - before sleep");
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                log.info(str + " was interrupted");
            }
            
            log.info(str + " - after sleep");
            
            return Play.Status.Success;
        }
        
        @Override
        public String toString() {
            return "SimpleExecutable[" + str + "]";
        }
    }
}

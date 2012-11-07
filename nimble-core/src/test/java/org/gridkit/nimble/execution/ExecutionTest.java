package org.gridkit.nimble.execution;

import java.util.concurrent.TimeUnit;

import org.gridkit.nimble.driver.Activity;
import org.junit.Test;

public class ExecutionTest {
    @Test
    public void simple_test() {
        ExecConfig c1 = (new ExecConfigBuilder())
                .tasks(new PrintTask("one"))
                .condition(ExecConditions.iterations(2))
                .continuous(false).build();
        
        ExecConfig c2 = (new ExecConfigBuilder())
                .tasks(new PrintTask("two"), new PrintTask("three"))
                .condition(ExecConditions.duration(1, TimeUnit.MILLISECONDS))
                .continuous(false)
                .build();
        
        ExecutionDriver driver = Execution.newDriver();
        
        TaskPool pool = driver.newTaskPool("TestTaskPool", 2);
        
        Activity a1 = pool.exec(c1);

        a1.join();
        
        pool.setThreadsNumber(2);
        
        Activity a2 = pool.exec(c2);
                
        a2.join();
        
        pool.shutdown();
    }
    
    private static class PrintTask extends AbstractTask {
        private final String msg;
        
        public PrintTask(String msg) {
            super(false);
            this.msg = msg;
        }

        @Override
        public void run() throws Exception {
            System.err.println(msg);
        }
    }
}

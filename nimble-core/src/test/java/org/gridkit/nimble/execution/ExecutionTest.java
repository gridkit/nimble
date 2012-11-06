package org.gridkit.nimble.execution;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class ExecutionTest {
    @Test
    public void simple_test() {
        ExecConfig c1 = (new ExecConfigBuilder())
                .tasks(new PrintTask("one"))
                .condition(ExecConditions.iterations(2))
                .threads(2).continuous(false).build();
        
        ExecConfig c2 = (new ExecConfigBuilder())
                .tasks(new PrintTask("two"), new PrintTask("three"))
                .condition(ExecConditions.duration(1, TimeUnit.MILLISECONDS))
                .threads(1).continuous(false)
                .build();
        
        ExecutionDriver driver = Execution.newDriver();
        
        ExecHandle h1 = driver.newExecution(c1);
        
        h1.start();
        
        h1.join();
        
        ExecHandle h2 = h1.proceed(c2);
        
        h2.start();
        
        h2.join();
        
        driver.shutdown();
    }
    
    private static class PrintTask extends AbstractTask {
        private final String msg;
        
        public PrintTask(String msg) {
            this.msg = msg;
        }

        @Override
        public void run() throws Exception {
            System.err.println(msg);
        }
    }
}

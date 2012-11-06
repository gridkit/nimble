package org.gridkit.nimble.execution;

import org.gridkit.nimble.driver.Activity;

public interface ExecHandle extends Activity {
    ExecHandle start();
        
    ExecHandle proceed(ExecConfig config);
}

package org.gridkit.nimble.btrace.ext;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import net.java.btrace.api.wireio.AbstractCommand;

public class ConfigureSessionCmd extends AbstractCommand {
    public ConfigureSessionCmd(int type, int rx, int tx) {
        super(type, rx, tx);
    }

    @Override
    public void write(ObjectOutput out) throws IOException {        
    }

    @Override
    public void read(ObjectInput in) throws ClassNotFoundException, IOException {        
    }

    @Override
    public boolean needsResponse() {
        return true;
    }
}

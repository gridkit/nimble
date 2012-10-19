package org.gridkit.nimble.btrace.ext;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import net.java.btrace.api.wireio.AbstractCommand;
import net.java.btrace.api.wireio.Command;

@Command(clazz = PingCmd.class)
public class PingCmd extends AbstractCommand {
    public PingCmd(int type, int rx, int tx) {
        super(type, rx, tx);
    }

    @Override
    public boolean needsResponse() {
        return true;
    }
    
    @Override
    public void write(ObjectOutput out) throws IOException {}

    @Override
    public void read(ObjectInput in) throws ClassNotFoundException, IOException {}
}

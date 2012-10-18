package org.gridkit.nimble.btrace.ext;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import net.java.btrace.api.wireio.AbstractCommand;
import net.java.btrace.api.wireio.Command;

@Command(clazz = PollSamplesCmd.class)
public class PollSamplesCmd extends AbstractCommand {    
    public PollSamplesCmd(int typeId, int rx, int tx) {
        super(typeId, rx, tx);
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

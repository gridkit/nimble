package org.gridkit.nimble.btrace.ext;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import net.java.btrace.api.wireio.AbstractCommand;
import net.java.btrace.api.wireio.Command;

@Command(clazz = PollSamplesCmd.class)
public class PollSamplesCmd extends AbstractCommand {    
    private String traceSriptClass;
    
    public PollSamplesCmd(int typeId, int rx, int tx) {
        super(typeId, rx, tx);
    }

    public String getTraceSriptClass() {
        return traceSriptClass;
    }

    public void setTraceSriptClass(String traceSriptClass) {
        this.traceSriptClass = traceSriptClass;
    }

    @Override
    public boolean needsResponse() {
        return true;
    }

    @Override
    public void write(ObjectOutput out) throws IOException {
        out.writeObject(traceSriptClass);
    }

    @Override
    public void read(ObjectInput in) throws ClassNotFoundException, IOException {
        traceSriptClass = (String)in.readObject();
    }
}

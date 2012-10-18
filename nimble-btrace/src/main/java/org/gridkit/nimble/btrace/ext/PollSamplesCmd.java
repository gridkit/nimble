package org.gridkit.nimble.btrace.ext;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import net.java.btrace.api.wireio.AbstractCommand;
import net.java.btrace.api.wireio.Command;

@Command(clazz = PollSamplesCmd.class)
public class PollSamplesCmd extends AbstractCommand {
    private Set<String> names;
    
    public PollSamplesCmd(int typeId, int rx, int tx) {
        super(typeId, rx, tx);
    }

    @Override
    public boolean needsResponse() {
        return true;
    }

    public Set<String> getNames() {
        return names;
    }

    public void setNames(Set<String> names) {
        this.names = names;
    }

    @Override
    public void write(ObjectOutput out) throws IOException {
        out.writeObject(names);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void read(ObjectInput in) throws ClassNotFoundException, IOException {
        names = (Set<String>)in.readObject();
    }
}

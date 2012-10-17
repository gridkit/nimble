package org.gridkit.nimble.btrace.ext;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import net.java.btrace.api.wireio.AbstractCommand;
import net.java.btrace.api.wireio.Command;

@Command(clazz = PollRingCmd.class)
public class PollRingCmd extends AbstractCommand {
    private Set<String> ids;
    
    public PollRingCmd(int typeId, int rx, int tx) {
        super(typeId, rx, tx);
    }

    @Override
    public boolean needsResponse() {
        return true;
    }
    
    public Set<String> getIds() {
        return ids;
    }
    
    public void setIds(Set<String> ids) {
        this.ids = ids;
    }
    
    @Override
    public void write(ObjectOutput out) throws IOException {
        out.writeObject(ids);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void read(ObjectInput in) throws ClassNotFoundException, IOException {
        ids = (Set<String>)in.readObject();
    }
}

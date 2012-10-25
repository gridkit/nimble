package org.gridkit.nimble.btrace.ext;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;

import net.java.btrace.api.wireio.AbstractCommand;
import net.java.btrace.api.wireio.Command;

@Command(clazz = ClearSamplesCmd.class)
public class ClearSamplesCmd extends AbstractCommand {
    private Collection<String> scriptClasses;
    
    public ClearSamplesCmd(int type, int rx, int tx) {
        super(type, rx, tx);
    }

    public Collection<String> getScriptClasses() {
        return scriptClasses;
    }

    public void setScriptClasses(Collection<String> scriptClasses) {
        this.scriptClasses = scriptClasses;
    }

    @Override
    public boolean needsResponse() {
        return true;
    }

    @Override
    public void write(ObjectOutput out) throws IOException {
        out.writeObject(scriptClasses);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void read(ObjectInput in) throws ClassNotFoundException, IOException {
        scriptClasses = (Collection<String>) in.readObject();
    }
}

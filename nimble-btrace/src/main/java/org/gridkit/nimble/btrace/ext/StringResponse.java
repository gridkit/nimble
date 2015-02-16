package org.gridkit.nimble.btrace.ext;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import net.java.btrace.api.wireio.ResponseCommand;

final public class StringResponse extends ResponseCommand<String> {

    public StringResponse(int typeId, int rx, int tx) {
        super(typeId, rx, tx);
    }

    @Override
    public void read(ObjectInput in) throws ClassNotFoundException, IOException {
        super.read(in);
        setPayload(in.readUTF());
    }

    @Override
    public void write(ObjectOutput  out) throws IOException {
        super.write(out);
        out.writeUTF(getPayload());
    }
}

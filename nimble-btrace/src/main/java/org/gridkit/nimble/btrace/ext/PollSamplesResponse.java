package org.gridkit.nimble.btrace.ext;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import net.java.btrace.api.wireio.ResponseCommand;

public class PollSamplesResponse extends ResponseCommand<PollSamplesCmdResult> {

    public PollSamplesResponse(int typeId, int rx, int tx) {
        super(typeId, rx, tx);
    }

    @Override
    public void read(ObjectInput in) throws ClassNotFoundException, IOException {
        super.read(in);
        setPayload((PollSamplesCmdResult)in.readObject());
    }

    @Override
    public void write(ObjectOutput  out) throws IOException {
        super.write(out);
        out.writeObject(getPayload());
    }
}

package org.gridkit.nimble.btrace.ext;

import net.java.btrace.api.wireio.Command;
import net.java.btrace.spi.wireio.ResponseImpl;

@Command(clazz=PollSamplesResponse.class)
public class PollSamplesResponseImpl extends ResponseImpl<PollSamplesResponse> {
}

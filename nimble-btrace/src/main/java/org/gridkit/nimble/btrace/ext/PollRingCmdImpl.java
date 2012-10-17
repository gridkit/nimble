package org.gridkit.nimble.btrace.ext;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.java.btrace.api.core.BTraceLogger;
import net.java.btrace.api.core.Lookup;
import net.java.btrace.api.wireio.Channel;
import net.java.btrace.api.wireio.Command;
import net.java.btrace.spi.wireio.CommandImpl;

@Command(clazz=PollRingCmd.class)
public class PollRingCmdImpl extends CommandImpl<PollRingCmd> {    
    @Override
    public void execute(Lookup ctx, PollRingCmd cmd) {        
        Map<String, List<RingBuffer.Element>> result = new HashMap<String, List<RingBuffer.Element>>();
        
        Channel channel = ctx.lookup(Channel.class);
        Map<String, RingBuffer> ringBuffers = Nimble.getRingBuffers();

        for (String id : cmd.getIds()) {
            RingBuffer ringBuffer = ringBuffers.get(id);
            
            if (ringBuffer != null) {
                result.put(id, ringBuffer.get());
            } else {
                result.put(id, Collections.<RingBuffer.Element>emptyList());
            }
        }
        
        try {
            channel.sendResponse(cmd, result);
        } catch (IOException e) {
            BTraceLogger.debugPrint(e);
        }
    }
}

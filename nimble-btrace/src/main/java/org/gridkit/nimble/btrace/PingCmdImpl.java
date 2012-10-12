package org.gridkit.nimble.btrace;

import java.io.IOException;

import net.java.btrace.api.core.BTraceLogger;
import net.java.btrace.api.core.Lookup;
import net.java.btrace.api.wireio.Channel;
import net.java.btrace.api.wireio.Command;
import net.java.btrace.spi.wireio.CommandImpl;

@Command(clazz = PingCmd.class)
public class PingCmdImpl extends CommandImpl<PingCmd> {
    @Override
    public void execute(Lookup ctx, PingCmd cmd) {
        Channel channel = ctx.lookup(Channel.class);
        
        try {
            channel.sendResponse(cmd, true);
        } catch (IOException e) {
            BTraceLogger.debugPrint(e);
        }
    }
}

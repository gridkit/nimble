package org.gridkit.nimble.btrace.ext;

import java.io.IOException;
import java.util.Collection;

import net.java.btrace.api.core.BTraceLogger;
import net.java.btrace.api.core.Lookup;
import net.java.btrace.api.wireio.Channel;
import net.java.btrace.api.wireio.Command;
import net.java.btrace.spi.wireio.CommandImpl;

@Command(clazz = ClearSamplesCmd.class)
public class ClearSamplesCmdImpl extends CommandImpl<ClearSamplesCmd> {
    @Override
    public void execute(Lookup ctx, ClearSamplesCmd cmd) {        
        Collection<ScriptStore> scriptStores = Nimble.getScriptStores(cmd.getScriptClasses());
        
        for (ScriptStore scriptStore : scriptStores) {
            for (SampleStore sampleStore : scriptStore.getSampleStores()) {
                sampleStore.clear();
            }
        }
        
        Channel channel = ctx.lookup(Channel.class);
        
        try {
            channel.sendResponse(cmd, true);
        } catch (IOException e) {
            BTraceLogger.debugPrint(e);
        }
    }
}

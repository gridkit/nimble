package org.gridkit.nimble.btrace.ext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.java.btrace.api.core.BTraceLogger;
import net.java.btrace.api.core.Lookup;
import net.java.btrace.api.wireio.Channel;
import net.java.btrace.api.wireio.Command;
import net.java.btrace.spi.wireio.CommandImpl;

import org.gridkit.nimble.btrace.ext.model.SampleStoreState;

@Command(clazz = PollStateCmd.class)
public class PollStateCmdImpl extends CommandImpl<PollStateCmd> {
    @Override
    public void execute(Lookup ctx, PollStateCmd cmd) {
        List<SampleStoreState> data = new ArrayList<SampleStoreState>();
        
        Collection<ScriptStore> scriptStores = Nimble.getScriptStores(cmd.getScriptClasses());
        
        for (ScriptStore scriptStore : scriptStores) {
            for (SampleStore sampleStore : scriptStore.getSampleStores()) {
                SampleStoreState state = new SampleStoreState();

                state.setScriptClass(scriptStore.getScriptClass());
                state.setSampleStore(sampleStore.getStoreName());
                state.setNextSeqNum(sampleStore.getNextSeqNum());
                
                data.add(state);
            }
        }

        PollStateCmdResult result = new PollStateCmdResult();
        result.setData(data);
        
        Channel channel = ctx.lookup(Channel.class);
        
        try {
            channel.sendResponse(cmd, result);
        } catch (IOException e) {
            BTraceLogger.debugPrint(e);
        }
    }
}

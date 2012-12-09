package org.gridkit.nimble.btrace.ext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.java.btrace.api.core.BTraceLogger;
import net.java.btrace.api.core.Lookup;
import net.java.btrace.api.wireio.Channel;
import net.java.btrace.api.wireio.Command;
import net.java.btrace.spi.wireio.CommandImpl;

import org.gridkit.nimble.btrace.ext.model.SampleStoreContents;

@Command(clazz=PollSamplesCmd.class)
public class PollSamplesCmdImpl extends CommandImpl<PollSamplesCmd> {    
    @Override
    public void execute(Lookup ctx, PollSamplesCmd cmd) {        
        List<SampleStoreContents> data = new ArrayList<SampleStoreContents>();
        
        ScriptStore scriptStore = Nimble.getScriptStore(cmd.getTraceSriptClass());
        
        if (scriptStore != null) {
            for (SampleStore sampleStore : scriptStore.getSampleStores()) {
                SampleStoreContents contents = new SampleStoreContents();

                contents.setSampleStore(sampleStore.getName());
                contents.setSamples(sampleStore.getSamples());
                
                data.add(contents);
            }
        }

        PollSamplesCmdResult result = new PollSamplesCmdResult();
        result.setData(data);
        
        Channel channel = ctx.lookup(Channel.class);
        
        try {
            channel.sendResponse(cmd, result);
        } catch (IOException e) {
            BTraceLogger.debugPrint(e);
        }
    }
}

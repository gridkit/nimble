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

@Command(clazz=PollSamplesCmd.class)
public class PollSamplesCmdImpl extends CommandImpl<PollSamplesCmd> {    
    @Override
    public void execute(Lookup ctx, PollSamplesCmd cmd) {        
        List<PollSamplesCmdResult.Element> elements = new ArrayList<PollSamplesCmdResult.Element>();
        
        Collection<ScriptStore> scriptStores = Nimble.getScriptStores(cmd.getScriptClasses());
        
        for (ScriptStore scriptStore : scriptStores) {
            for (SampleStore sampleStore : scriptStore.getSampleStores()) {
                PollSamplesCmdResult.Element element = new PollSamplesCmdResult.Element();

                element.setScriptClass(scriptStore.getScriptClass());
                element.setSampleStore(sampleStore.getName());
                element.setSamples(sampleStore.getSamples());
                
                elements.add(element);
            }
        }
        
        PollSamplesCmdResult result = new PollSamplesCmdResult();
        result.setElements(elements);
        
        Channel channel = ctx.lookup(Channel.class);
        
        try {
            channel.sendResponse(cmd, result);
        } catch (IOException e) {
            BTraceLogger.debugPrint(e);
        }
    }
}

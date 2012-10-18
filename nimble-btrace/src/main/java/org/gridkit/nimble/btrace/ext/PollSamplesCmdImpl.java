package org.gridkit.nimble.btrace.ext;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import net.java.btrace.api.core.BTraceLogger;
import net.java.btrace.api.core.Lookup;
import net.java.btrace.api.wireio.Channel;
import net.java.btrace.api.wireio.Command;
import net.java.btrace.spi.wireio.CommandImpl;

import org.gridkit.nimble.btrace.ext.model.Sample;

@Command(clazz=PollSamplesCmd.class)
public class PollSamplesCmdImpl extends CommandImpl<PollSamplesCmd> {    
    @Override
    public void execute(Lookup ctx, PollSamplesCmd cmd) {        
        ConcurrentMap<String, SampleStore<?>> sampleStores = Nimble.getSampleStores();
        
        Map<String, PollSamplesCmdResult<?>> result = new HashMap<String, PollSamplesCmdResult<?>>();
        
        Channel channel = ctx.lookup(Channel.class);

        Set<String> names = getNames(cmd, sampleStores);
        
        for (String name : names) {
            SampleStore<?> sampleStore = sampleStores.get(name);
            
            if (sampleStore != null) {
                put(name, sampleStore, result);
            }
        }
        
        try {
            channel.sendResponse(cmd, result);
        } catch (IOException e) {
            BTraceLogger.debugPrint(e);
        }
    }
    
    private static Set<String> getNames(PollSamplesCmd cmd, ConcurrentMap<String, SampleStore<?>> sampleStores) {
        if (cmd.getNames() != null) {
            return cmd.getNames();
        } else {
            Set<String> result = new HashSet<String>();
            result.addAll(sampleStores.keySet());
            return result;
        }
    }
    
    @SuppressWarnings("unchecked")
    private static void put(String name, SampleStore<?> sampleStore, Map<String, PollSamplesCmdResult<?>> result) {
        result.put(name, new PollSamplesCmdResult<Sample>((SampleStore<Sample>)sampleStore));
    }
}

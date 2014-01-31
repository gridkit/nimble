package org.gridkit.nimble.util;

import static org.gridkit.nimble.util.StringOps.F;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.gridkit.nanocloud.Cloud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllocationManager {
    private static final Logger log = LoggerFactory.getLogger(AllocationManager.class);

    public interface Context {
        void allocate(HostSource source, int nHosts, int nNodes, String... labels);
        
        void allocate(HostSource source, int nHosts, int nNodes, List<String> labels);
        
        HostSource newExclusiveHostSource(Collection<String> hosts);
        
        HostSource newRoundHostSource(Collection<String> hosts);
    }
    
    public interface Executor {
        void run(Context context);
    }
    
    public interface NodeFactory {
        Cloud newViNodeSet();
    }
    
    public static interface HostSource {
        List<String> getHosts(int count);
    }

    public static Cloud allocate(Cloud nodeSet, Executor executor) {
        executor.run(new ContextImpl(nodeSet));
        return nodeSet;
    }
    
    public static Cloud allocate(NodeFactory factory, int retries, Executor executor) {
        if (retries < 0) {
            throw new IllegalArgumentException("retries < 0");
        }
        
        boolean success = false;
        Cloud nodeSet = null;
        int triesLeft = retries + 1;
        
        while (triesLeft-- > 0 && !success) {
            nodeSet = factory.newViNodeSet();
            
            allocate(nodeSet, executor);
            
            success = valid(nodeSet);
            
            if (!success) {
                shutdown(nodeSet);
            }
        }
        
        if (!success) {
            throw new RuntimeException("Failed to create ViNodeSet with " + retries + " retries");
        } else {
            return nodeSet;
        }
    }
    
    private static class ContextImpl implements Context{
        private Cloud nodeSet;
        private Map<Set<String>, Integer> indexes = new HashMap<Set<String>, Integer>();
        
        public ContextImpl(Cloud nodeSet) {
            this.nodeSet = nodeSet;
        }

        @Override
        public void allocate(HostSource source, int nHosts, int nNodes, String... labels) {
            allocate(source, nHosts, nNodes, Arrays.asList(labels));
        }
        
        @Override
        public void allocate(HostSource source, int nHosts, int nNodes, List<String> labels) {
            List<String> hosts = source.getHosts(nHosts);
            
            for (String host : hosts) {
                Set<String> tags = new HashSet<String>(labels);
                tags.add(host);

                for (int node = 0; node < nNodes; ++node) {
                    int index = nextIndex(tags);
                    String name = name(host, labels, index);
                    nodeSet.node(name);
                }
            }
        }
        
        @Override
        public HostSource newExclusiveHostSource(Collection<String> hosts) {
            return new ExclusiveHostSource(hosts);
        }
        
        @Override
        public HostSource newRoundHostSource(Collection<String> hosts) {
            return new RoundHostSource(hosts);
        }
        
        private Integer nextIndex(Set<String> tags) {                
            Integer index = indexes.get(tags);
            
            if (index == null) {
                index = 0;
            }
            
            indexes.put(tags, index + 1);
            
            return index;
        }
    }

    private static boolean valid(Cloud nodeSet) {
        Future<?> f  = nodeSet.node("**").submit(new Runnable() {
            @Override
            public void run() {}
        });

        try {
            f.get();
            return true;
        } catch(Exception e) {
            log.error("Failed to test ViNodeSet", e);
            return false;
        }
    }
    
    private static void shutdown(Cloud nodeSet) {
        try {
            nodeSet.shutdown();
        } catch(Exception e) {
            log.trace("Exception while shutting down ViNodeSet", e);
        }
    }
   
    private static String name(String host, List<String> labels, int index) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(host).append(".");
        
        for(String label : labels) {
            sb.append(label).append(".");
        }
        
        sb.append(index);
        
        return sb.toString();
    }
    
    public static String pattern(String label) {
        return "**." + label + ".**";
    }
        
    private static class ExclusiveHostSource implements HostSource {
        private List<String> hosts;

        public ExclusiveHostSource(Collection<String> hosts) {
            this.hosts = new ArrayList<String>(hosts);
        }

        @Override
        public List<String> getHosts(int count) {
            if (count > hosts.size()) {
                throw new IllegalStateException(F("Can't get %d hosts", count));
            }
            
            List<String> result = new ArrayList<String>(hosts.subList(0, count));
                        
            if (count == hosts.size()) {
                hosts = Collections.emptyList();
            } else {
                hosts = hosts.subList(count, hosts.size());
            }

            return result;
        }
    }
    
    private static class RoundHostSource implements HostSource {
        private List<String> hosts;
        private Iterator<String> iter;

        public RoundHostSource(Collection<String> hosts) {
            this.hosts = new ArrayList<String>(hosts);
            this.iter = this.hosts.iterator();
        }

        @Override
        public List<String> getHosts(int count) {
            if (count < 0) {
                throw new IllegalArgumentException("count < 0");
            }
            
            List<String> result = new ArrayList<String>(count);
            
            while (count-- > 0) {
                result.add(getNextHost());
            }

            return result;
        }
        
        private String getNextHost() {
            if (hosts.isEmpty()) {
                throw new IllegalStateException("No hosts found");
            }
            
            if (!iter.hasNext()) {
                iter = hosts.iterator();
            }
            
            return iter.next();
        }
    }
}

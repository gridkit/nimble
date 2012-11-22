package org.gridkit.nimble.probe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.gridkit.lab.jvm.attach.AttachManager;
import org.gridkit.lab.jvm.attach.JavaProcessId;
import org.gridkit.lab.jvm.attach.JavaProcessMatcher;

public class JvmMatcherPidProvider implements PidProvider, Serializable {
    private static final long serialVersionUID = -1550756426157493470L;
    
    private final JavaProcessMatcher matcher;

    public JvmMatcherPidProvider(JavaProcessMatcher matcher) {
        this.matcher = matcher;
    }

    @Override
    public Collection<Long> getPids() {
        List<Long> pids = new ArrayList<Long>();

        for(JavaProcessId jpid: AttachManager.listJavaProcesses(matcher)) {
        	pids.add(jpid.getPID());
        }
        
        Collections.shuffle(pids);
        
        return pids;
    }
    
    @Override
    public String toString() {
    	return matcher.toString();
    }
}

package org.gridkit.nimble.probe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.gridkit.nimble.sensor.JvmMatcher;
import org.gridkit.nimble.util.JvmOps;

import com.sun.tools.attach.VirtualMachineDescriptor;

public class JvmMatcherPidProvider implements PidProvider, Serializable {
    private static final long serialVersionUID = -1550756426157493470L;
    
    private final JvmMatcher matcher;

    public JvmMatcherPidProvider(JvmMatcher matcher) {
        this.matcher = matcher;
    }

    @Override
    public Collection<Long> getPids() {
        List<Long> pids = new ArrayList<Long>();
        
        List<VirtualMachineDescriptor> vms = new ArrayList<VirtualMachineDescriptor>(JvmOps.listVms(matcher));
        
        for(VirtualMachineDescriptor vm: vms) {
            pids.add(Long.parseLong(vm.id()));
        }
        
        Collections.shuffle(pids);
        
        return pids;
    }
}

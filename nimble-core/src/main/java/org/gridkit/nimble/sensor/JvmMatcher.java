package org.gridkit.nimble.sensor;

import static org.gridkit.nimble.util.StringOps.F;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gridkit.nimble.util.JvmOps;

import com.sun.tools.attach.VirtualMachineDescriptor;

public interface JvmMatcher {

	public boolean matches(Object vm); 

	
	@SuppressWarnings("serial")
	public static class PatternJvmMatcher implements JvmMatcher, Serializable {        
	    
	    private final Map<String, Pattern> patterns = new LinkedHashMap<String, Pattern>();
	
	    public void matchVmName(String pattern) {
	    	matchProp(":name", pattern);
	    }
	    
	    public void matchProp(String prop, String pattern) {
	    	Pattern p = Pattern.compile(pattern);
	    	patterns.put(prop, p);
	    }

	    public void matchPropExact(String prop, String pattern) {
	    	matchProp(prop, Pattern.quote(pattern));
	    }
	
	    @Override
		public boolean matches(Object vmobj) {
	    	VirtualMachineDescriptor vm = (VirtualMachineDescriptor) vmobj;
        	if (patterns.containsKey(":name")) {
        		if (!match(":name", vm.displayName())) {
        			return false;
        		}
        	}
            
            Properties props = JvmOps.getProps(vm);
            if (props == null) {
            	return false;
            }
            
            for(String prop: patterns.keySet()) {
            	if (!prop.startsWith(":")) {
            		if (!match(prop, props.getProperty(prop))) {
            			return false;
            		}
            	}
            }
	            
	        return true;
	    }
	    
	    private boolean match(String prop, String value) {
	    	if (value == null) {
	    		return false;
	    	}
			Matcher matcher = patterns.get(prop).matcher(value);
			return matcher.matches();
		}


		@Override
	    public String toString() {
	        return F("%s%s", getClass().getSimpleName(), patterns.toString());
	    }
	}	
}

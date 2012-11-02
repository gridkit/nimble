package org.gridkit.lab.metering.common;

import org.gridkit.lab.metering.AttrName;
import org.gridkit.lab.metering.Observed;

public interface ObservedProcess extends Observed {
	
	public static AttrName HOSTNAME = AttrName.attr("hostname");	
	public static AttrName PID = AttrName.attr("pid");	
	
	public String hostname();
	
	public long pid();

}

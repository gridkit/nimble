package org.gridkit.lab.metering.common;

import org.gridkit.lab.metering.AttrName;
import org.gridkit.lab.metering.Observed;

public interface ObservedHost extends Observed {
	
	public static AttrName HOSTNAME = AttrName.attr("hostname");
	
	public String hostname();

}

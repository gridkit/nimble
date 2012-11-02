package org.gridkit.lab.metering.common;

import org.gridkit.lab.metering.AttrName;
import org.gridkit.lab.metering.Observed;

public interface ObservedNetInterface extends Observed {
	
	public static AttrName HOSTNAME = AttrName.attr("hostname");
	public static AttrName IFNAME = AttrName.attr("ifname");
	
	public String hostname();
	
	public String ifname();

}

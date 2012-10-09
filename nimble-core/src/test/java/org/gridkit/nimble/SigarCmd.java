package org.gridkit.nimble;

import org.gridkit.lab.sigar.SigarFactory;
import org.hyperic.sigar.NetStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.cmd.Top;
import org.junit.Ignore;

@Ignore
public class SigarCmd {
    public static void main(String[] args) throws Exception {
        Sigar sigar = SigarFactory.newSigar();
        
        NetStat ns = new NetStat();
        ns.stat(sigar);
        System.out.println(ns.getTcpListen());
        
        Top.main(args);
    }
}

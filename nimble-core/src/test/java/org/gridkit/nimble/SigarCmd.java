package org.gridkit.nimble;

import org.gridkit.lab.sigar.SigarFactory;
import org.hyperic.sigar.cmd.Top;
import org.junit.Ignore;

@Ignore
public class SigarCmd {
    public static void main(String[] args) throws Exception {
        SigarFactory.newSigar();      
        Top.main(args);
    }
}

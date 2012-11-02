package org.gridkit.nimble.probe.sigar;

import org.gridkit.nimble.metering.SampleKey;

public final class SigarMeasure implements SampleKey {
	
    public static final String PROBE_KEY   = "sigar_probe";
    
    public static final String SYS_NET_PROBE  = "sysNet";
    public static final String SYS_MEM_PROBE  = "sysMem";
    public static final String SYS_CPU_PROBE  = "sysCpu";
    public static final String PROC_CPU_PROBE = "procCpu";
    public static final String PROC_MEM_PROBE = "procMem";
    
    public static final String MEASURE_KEY = "sigar_measure";
    
    public static final String PID_KEY           = "pid";
    public static final String NET_INTERFACE_KEY = "netInterface";
    
    public static final String CPU_USER    = "cpu_user";
    public static final String CPU_NICE    = "cpu_nice";
    public static final String CPU_SYSTEM  = "cpu_sys";
    public static final String CPU_IDLE    = "cpu_idle";
    public static final String CPU_WAIT    = "cpu_wait";
    public static final String CPU_IRQ     = "cpu_irq";
    public static final String CPU_SOFTIRQ = "cpu_softIrq";
    public static final String CPU_STOLEN  = "cpu_stolen";
    public static final String CPU_TOTAL   = "cpu_total";

    public static final String MEM_SIZE        = "mem_size";
    public static final String MEM_RESIDENT    = "mem_resident";
    public static final String MEM_SHARE       = "mem_share";
    
    public static final String MEM_RAM         = "mem_ram";
    public static final String MEM_USED        = "mem_used";
    public static final String MEM_FREE        = "mem_free";
    public static final String MEM_ACTUAL_USED = "mem_actualUsed";
    public static final String MEM_ACTUAL_FREE = "mem_actualFree";
    public static final String MEM_TOTAL       = "mem_total";
    
    public static final String NET_RX_BYTES      = "net_rxBytes";
    public static final String NET_RX_PACKETS    = "net_rxPackets";
    public static final String NET_RX_ERRORS     = "net_rxErrors";
    public static final String NET_RX_DROPPED    = "net_rxDropped";
    
    public static final String NET_TX_BYTES      = "net_txBytes";
    public static final String NET_TX_PACKETS    = "net_txPackets";
    public static final String NET_TX_ERRORS     = "net_txErrors";
    public static final String NET_TX_DROPPED    = "net_txDropped";
    
    private SigarMeasure() {}
}

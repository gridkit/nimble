package org.gridkit.nimble.btrace;

import net.java.btrace.client.Client;

public interface BTraceClientSource {
    Client getClient(long pid) throws ClientCreateException;
}

package org.gridkit.nimble.btrace;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import net.java.btrace.api.core.ServiceLocator;
import net.java.btrace.api.extensions.ExtensionsRepository;
import net.java.btrace.api.wireio.Channel;
import net.java.btrace.api.wireio.CommandFactory;
import net.java.btrace.api.wireio.ObjectInputStreamEx;
import net.java.btrace.client.ClientChannel;
import net.java.btrace.spi.wireio.CommandImpl;

public class NimbleClientChannel extends ClientChannel {
    private static final String AGENT_EXCLUDE_PREFIX = net.java.btrace.agent.wireio.ExitCommandImpl.class.getPackage().getName();
    
    public static Channel open(Socket skt, ExtensionsRepository extRep) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(skt.getOutputStream());
            ObjectInputStream ois = new ObjectInputStreamEx(skt.getInputStream(), extRep.getClassLoader());
            
            NimbleClientChannel ch = new NimbleClientChannel(ois, oos, extRep);
            if (!ch.handshake()) {
                try {
                    ch.output.close();
                    ch.input.close();
                } catch (IOException e) {
                }
                return null;
            }
            return ch;
        } catch (IOException e) {
        }
        return null;
    }
    
    protected NimbleClientChannel(ObjectInput oi, ObjectOutput oo, ExtensionsRepository extRep) {
        super(oi, oo, extRep);
    }

    @Override
    protected CommandFactory newCommandFactory() {
        ClassLoader cl = extRep.getClassLoader(getMyLoader());        

        @SuppressWarnings("rawtypes")
        Iterable<CommandImpl> allImpls = ServiceLocator.listServices(CommandImpl.class, cl);
        
        @SuppressWarnings("rawtypes")
        List<CommandImpl> clientImpls = new ArrayList<CommandImpl>();
        
        for (@SuppressWarnings("rawtypes") CommandImpl impl : allImpls) {
            if (!impl.getClass().getName().startsWith(AGENT_EXCLUDE_PREFIX)) {
                clientImpls.add(impl);
            }
        }

        return CommandFactory.getInstance(clientImpls);
    }
}

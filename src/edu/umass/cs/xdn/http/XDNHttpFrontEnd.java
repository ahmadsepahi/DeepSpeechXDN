package edu.umass.cs.xdn.http;

import edu.umass.cs.reconfiguration.interfaces.ActiveReplicaFunctions;

import java.net.InetSocketAddress;

public class XDNHttpFrontEnd {

    public XDNHttpFrontEnd(ActiveReplicaFunctions arf,
                           InetSocketAddress addr,
                           boolean ssl){
        System.out.println("########## Bootup XDN HTTP front end ##########");
    }

    public static void main(String[] args) {
    }
}

package de.tum.i13.shared;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public interface CommandProcessor {

    String process(String command);

    String connectionAccepted(InetSocketAddress address, InetSocketAddress remoteAddress);

    void connectionClosed(InetAddress address);
}

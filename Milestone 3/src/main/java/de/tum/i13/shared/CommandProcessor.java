package de.tum.i13.shared;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public interface CommandProcessor {

    String process(String command);

    String connectionAccepted(InetSocketAddress address, InetSocketAddress remoteAddress);

    void connectionClosed(InetAddress address);
}

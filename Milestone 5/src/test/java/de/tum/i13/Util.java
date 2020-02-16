package de.tum.i13;


import java.io.IOException;
import java.net.ServerSocket;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class Util {

    public static int getFreePort() throws IOException {

        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}

package de.tum.i13;

import de.tum.i13.shared.LogeLevelChange;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.*;

public class Util {

    public static int getFreePort() throws IOException {

        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}

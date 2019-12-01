package de.tum.i13.shared;

import java.io.IOException;
import java.net.ServerSocket;
/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class Utility {

    /**
     * Used to print an error message
     */
    public static void retUnknownCommand() {
        printEchoLine("Unknown command");
    }

    /**
     * Used to print a message
     * @param msg message that we want to print
     */
    public static void printEchoLine(String msg) {
        System.out.print("EchoClient> " + msg + "\n");
    }



    public static String byteToHex(byte[] in) {
        StringBuilder sb = new StringBuilder();
        for(byte b : in) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static int getFreePort() throws IOException {

        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}

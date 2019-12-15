package de.tum.i13.shared;

import java.io.IOException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
     *
     * @param msg message that we want to print
     */
    public static void printEchoLine(String msg) {
        System.out.print("EchoClient> " + msg + "\n");
    }

    /**
     * This function is used to compute the hash of the ip, port
     *
     * @param ip
     * @param port
     * @return
     */
    public static String computeHash(String ip, int port) {

        MessageDigest md = null;
        try {
            String toHash = ip +
                    port;
            md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(toHash.getBytes());
            return byteToHex(messageDigest);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * This method is called to compute the hash of a key
     * @param key The key for which we want to compute the hash
     * @return the computed hash
     */
    public static String computeHash(String key) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(key.getBytes());
            return byteToHex(messageDigest);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * @param s1
     * @param s2
     * @return -1: if s1 is less than s2
     * 0: the two values are the same
     * 1: if s1 is greater than s2
     */
    public static int compareHex(String s1, String s2) {
        BigInteger b1 = new BigInteger(s1, 16);
        BigInteger b2 = new BigInteger(s2, 16);

        return b1.compareTo(b2);
    }

    /**
     * This method is called to convert a byte[] to a String
     * @param in input byte[]
     * @return the string that we obtained
     */
    public static String byteToHex(byte[] in) {
        StringBuilder sb = new StringBuilder();
        for (byte b : in) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    /**
     * This method returns the first free port that we find
     * @return the free port
     * @throws IOException
     */
    public static int getFreePort() throws IOException {

        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    /**
     * This method is used to encode a String
     * @param string the string that we want to encode
     * @return the encoded string
     */
    public static String encode(String string) {
        byte[] byteArray = string.getBytes();
        return byteToHex(byteArray);
    }
}

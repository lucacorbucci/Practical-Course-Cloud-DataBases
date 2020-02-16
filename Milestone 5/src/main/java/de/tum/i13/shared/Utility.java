package de.tum.i13.shared;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

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
     *
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
     *
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
     *
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
     *
     * @param string the string that we want to encode
     * @return the encoded string
     */
    public static String encode(String string) {
        byte[] byteArray = string.getBytes();
        return byteToHex(byteArray);
    }

    /**
     * This is used to decode a received message
     *
     * @param string the encoded string
     * @return the decoded string
     */
    public static String decode(String string) {
        byte[] a = hexToByte(string);
        return new String(a);
    }

    private static byte[] hexToByte(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            int j = Integer.parseInt(hex.substring(index, index + 2), 16);
            bytes[i] = (byte) j;
        }

        return bytes;
    }

    /**
     * This method creates an array of open Socket. The Sockets are connected to the nodes of the parameter Array
     *
     * @param nodes List of SocketAddress
     * @return List of open sockets
     */
    public static ArrayList<Pair<Socket, Pair<ObjectInputStream, ObjectOutputStream>>> openSocket(ArrayList<SocketAddress> nodes) throws IOException {
        ArrayList<Pair<Socket, Pair<ObjectInputStream, ObjectOutputStream>>> socketList = new ArrayList<>();
        for (SocketAddress s : nodes) {
            Socket socket = new Socket();
            socket.connect(s);
            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            oos.flush();
            ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
            socketList.add(new Pair<>(socket, new Pair<>(ois, oos)));
        }
        return socketList;
    }
}

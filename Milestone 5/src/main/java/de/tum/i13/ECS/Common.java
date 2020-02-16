package de.tum.i13.ECS;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static de.tum.i13.shared.Utility.byteToHex;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class Common {

    /**
     * This function is used to compute the hash of the servers that joins
     * the network
     *
     * @param ip   ip of the server
     * @param port port of the server
     * @return the new hash or null if an exception was thrown
     */
    public static String computeHash(String ip, int port) {
        MessageDigest md = null;
        try {
            StringBuilder str = new StringBuilder();
            str.append(ip);
            str.append(port);
            String toHash = str.toString();
            md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(toHash.getBytes());

            return byteToHex(messageDigest);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }


}

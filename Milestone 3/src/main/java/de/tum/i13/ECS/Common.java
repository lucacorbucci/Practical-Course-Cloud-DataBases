package de.tum.i13.ECS;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static de.tum.i13.shared.Utility.byteToHex;
/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class Common {
    public static String computeHash(String ip, int port){
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

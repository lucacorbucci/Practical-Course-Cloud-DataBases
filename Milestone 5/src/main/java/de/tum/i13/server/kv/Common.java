package de.tum.i13.server.kv;

import de.tum.i13.shared.DataMap;
import de.tum.i13.shared.Metadata;
import de.tum.i13.shared.Pair;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class Common {

    /**
     * This method is called when we have to send a k,v pair to one of the replica server
     *
     * @param key         key of the pair that we want to send
     * @param value       value that we want to send
     * @param myHash      hash of the server
     * @param numReplicas number of replicas
     * @param metadata    current metadata
     */
    static void sendReplica(String key, String value, String myHash, int numReplicas, Metadata metadata) {
        Map.Entry<String, DataMap> successor = metadata.getMySuccessor(myHash);
        String successorIp = successor.getValue().getIp();
        int successorPort = successor.getValue().getIntraPort();
        try {
            Socket s = new Socket(successorIp, successorPort);
            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
            oos.flush();
            ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(s.getInputStream()));
            write("RECEIVE_SINGLE_REPLICA".length(), "RECEIVE_SINGLE_REPLICA", oos);
            oos.writeObject(new Pair<>(numReplicas, new Pair<>(key, value)));
            oos.flush();

            oos.close();
            ois.close();
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * This function is used to send a list of KVPair to another server
     *
     * @param list List of kvPairs
     * @param oos  ObjectOutputStream of the server that will receive the KVPairs
     * @param type Operation's type
     * @throws IOException
     */
    static void sendKVPairs(ArrayList<Pair<Integer, Pair<String, Pair<String, String>>>> list, ObjectOutputStream oos, String type) throws IOException {
        oos.flush();
        oos.writeLong(type.length());
        oos.writeUTF(type);
        oos.writeObject(list);
        oos.flush();
    }


    /**
     * This function is used to write to a server
     *
     * @param len    Len of the message that we want to send
     * @param toSend Message that we want to send
     * @param oos    ObjectOutputStream where we will send the message
     * @throws IOException
     */
    static void write(long len, String toSend, ObjectOutputStream oos) throws IOException {
        oos.flush();
        oos.writeLong(len);
        oos.writeUTF(toSend);
        oos.flush();
    }
}

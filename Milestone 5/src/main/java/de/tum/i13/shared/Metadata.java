package de.tum.i13.shared;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.logging.Logger;


/**
 * This is the class Metadata, we use this class to store all the
 * data about the servers that are currently active in the network.
 * For each server we store:
 * - The hash of the ip+port
 * A DataMap containing:
 * - The ip of the server
 * - The port of the server
 * - The hash of the first file for which this server is responsible
 * - The hash of the last file for which this server is responsible
 *
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class Metadata implements Serializable {
    private TreeMap<String, DataMap> map = new TreeMap<>(new ComparatorMetadata());

    public Logger logger;

    public Metadata(TreeMap<String, DataMap> map) {
        this.map = map;
    }

    public Metadata(Logger logger) {
        this.logger = logger;
    }

    public Metadata(String s, Logger logger) throws InvalidParameterException {
        this.logger = logger;

        ArrayList<String> data = new ArrayList<>(Arrays.asList(s.split(";")));
        data.forEach((d) -> {
            String[] singleData = d.split(",");
            if (singleData.length != 3) throw new InvalidParameterException();

            String[] ipAndPort = singleData[2].split(":");
            if (ipAndPort.length != 2) throw new InvalidParameterException();

            // String ip, int port, String startIndex, String endIndex, int intraPort
            map.putIfAbsent(singleData[1], new DataMap(ipAndPort[0], Integer.parseInt(ipAndPort[1]), singleData[0], singleData[1], -1));
        });
    }

    /**
     * @param index
     * @param replica
     * @return
     * @throws NoSuchElementException
     */
    public Pair<String, Integer> getReplica(String index, int replica) throws NoSuchElementException {
        Optional<DataMap> data = Optional.empty();
        // Replica == 1 means that the server is the first replica and 2 means that it is the second replica
        if (replica == 1 || replica == 2) {
            if (map == null) {
                throw new NoSuchElementException();
            }

            try {
                SortedMap<String, DataMap> m = map.tailMap(index);
                if (m.isEmpty()) {
                    data = map.values().stream().skip(replica).findFirst();
                } else {
                    if (m.size() >= replica + 1) {
                        data = m.values().stream().skip(replica).findFirst();
                    } else {
                        int size = replica - m.size();
                        data = map.values().stream().skip(size).findFirst();
                    }
                }
            } catch (NullPointerException e) {
                throw new NullPointerException();
            }
        }
        return data.map(dataMap -> new Pair<>(dataMap.getIp(), dataMap.getPort())).orElseGet(() -> new Pair<>(null, null));
    }

    private ArrayList<String> getReplicaHash(String hash, int numReplica) {
        ArrayList<String> replicas = new ArrayList<>();
        String currentHash = hash;
        String successorHash = null;
        if (map.size() > numReplica) {
            for (int i = 0; i < numReplica; i++) {
                successorHash = getMySuccessor(currentHash).getKey();
                replicas.add(successorHash);
                currentHash = successorHash;
            }
        }


        return replicas;

    }

    /**
     * @param serverHash
     * @param kvHash
     * @return
     */
    public boolean isReplica(String serverHash, String kvHash) {
        String responsibleServer = getResponsibleHash(kvHash);
        return getReplicaHash(responsibleServer, Constants.NUM_REPLICAS).contains(serverHash);
    }

    /**
     * @param kvHash
     * @return
     */
    private String getResponsibleHash(String kvHash) {
        if (map == null) {
            throw new NoSuchElementException();
        }
        String hash = null;
        try {
            hash = map.ceilingKey(kvHash);
            if (hash == null) {
                return map.firstKey();
            }
        } catch (NullPointerException e) {
            throw new NullPointerException();
        }

        return hash;
    }

    /**
     * This function is called when a server wants to find its successor
     * on the ring
     *
     * @param hash hash of the server
     * @return Successor's data
     */
    public Map.Entry<String, DataMap> getMySuccessor(String hash) {
        Map.Entry<String, DataMap> successorHash;
        successorHash = map.higherEntry(hash);
        if (successorHash == null)
            successorHash = map.firstEntry();

        return successorHash;
    }

    public Pair<String, SocketAddress> getSuccessorAddress(String hash) {
        Map.Entry<String, DataMap> firstSuccessor = getMySuccessor(hash);
        String hashFirstSuccessor = firstSuccessor.getKey();
        String ipFirstSuccessor = firstSuccessor.getValue().getIp();
        int portFirstSuccessor = firstSuccessor.getValue().getIntraPort();
        return new Pair<>(hashFirstSuccessor, new InetSocketAddress(ipFirstSuccessor, portFirstSuccessor));
    }

    public ArrayList<SocketAddress> getSuccessors(String h) {
        ArrayList<SocketAddress> addresses = new ArrayList<>();
        String hash = h;
        for (int i = 0; i < Constants.NUM_REPLICAS; i++) {
            Pair<String, SocketAddress> p = getSuccessorAddress(hash);
            hash = p.getFirst();
            addresses.add(p.getSecond());
        }
        return addresses;
    }

    public Map.Entry<String, DataMap> getMyPredecessorEntry(String hash) {
        Map.Entry<String, DataMap> predecessorHash = map.lowerEntry(hash);
        if (predecessorHash == null)
            predecessorHash = map.lastEntry();

        return predecessorHash;
    }

    public Pair<String, String> getMyPredecessor(String hash) {
        Map.Entry<String, DataMap> successorHash = map.lowerEntry(hash);
        if (successorHash == null)
            successorHash = map.lastEntry();

        return new Pair<>(successorHash.getValue().getStartIndex(), successorHash.getValue().getEndIndex());
    }


    public void addAll(Metadata m) {
        ArrayList<Pair<String, DataMap>> mm = m.getAll();
        map.clear();
        mm.forEach(p -> map.putIfAbsent(p.getFirst(), p.getSecond()));
    }


    public void print() {
        map.forEach((k, v) -> System.out.println(k));
    }

    public String popFirstKey() {
        String key = map.firstEntry().getKey();
        map.remove(key);
        return key;
    }

    /**
     * This function return the ip and the port of the server
     * that is responsible for the file identified with the hash passed
     * as parameter
     *
     * @param index the hash of the file that we want to access
     * @return A pair <IP, port>
     */
    public Pair<String, Integer> getResponsible(String index) throws NoSuchElementException, NullPointerException {
        if (map == null) {
            throw new NoSuchElementException();
        }
        Map.Entry<String, DataMap> m = null;
        try {
            m = map.ceilingEntry(index);

        } catch (NullPointerException e) {
            throw new NullPointerException();
        }

        DataMap dm = (m == null) ? map.firstEntry().getValue() : m.getValue();

        return new Pair<>(dm.getIp(), dm.getPort());
    }

    public int size() {
        return map.size();
    }


    public String getRangeHash(String index) throws NoSuchElementException {
        if (map == null) {
            throw new NoSuchElementException();
        }
        Map.Entry<String, DataMap> m = null;
        try {
            m = map.ceilingEntry(index);
        } catch (NullPointerException e) {
            throw new NullPointerException();
        }

        String hash = (m == null) ? map.firstEntry().getKey() : m.getKey();

        return hash;
    }

    /**
     * @return the String with all the data contained in the metadata table
     * the format of the string is:
     * StartHash, EndHash, ip:port;
     */
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        map.forEach((s, d) -> {
            strBuilder.append(d.getStartIndex());
            strBuilder.append(",");
            strBuilder.append(d.getEndIndex());
            strBuilder.append(",");
            strBuilder.append(d.getIp());
            strBuilder.append(":");
            strBuilder.append(d.getPort());
            strBuilder.append(";");
        });
        strBuilder.append("\r\n");
        return strBuilder.toString();
    }


    /**
     * @return the String with all the data contained in the metadata table
     * the format of the string is:
     * StartHash, EndHash, ip:port;
     */
    public String toStringReplicas() {
        Map.Entry<String, DataMap> next = null;
        Map.Entry<String, DataMap> nextNext = null;
        boolean complete = false;
        if (map.size() >= 3) {
            complete = true;
            Map.Entry<String, DataMap> entry = map.firstEntry();
            next = map.higherEntry(entry.getKey());
            nextNext = map.higherEntry(next.getKey());
        }

        StringBuilder strBuilder = new StringBuilder();

        for (Map.Entry<String, DataMap> entry : map.entrySet()) {

            DataMap d = entry.getValue();
            strBuilder.append(d.getStartIndex());
            strBuilder.append(",");
            strBuilder.append(d.getEndIndex());
            strBuilder.append(",");
            strBuilder.append(d.getIp());
            strBuilder.append(":");
            strBuilder.append(d.getPort());
            strBuilder.append(";");

            if (complete) {

                strBuilder.append(d.getStartIndex());
                strBuilder.append(",");
                strBuilder.append(d.getEndIndex());
                strBuilder.append(",");
                strBuilder.append(next.getValue().getIp());
                strBuilder.append(":");
                strBuilder.append(next.getValue().getPort());
                strBuilder.append(";");

                strBuilder.append(d.getStartIndex());
                strBuilder.append(",");
                strBuilder.append(d.getEndIndex());
                strBuilder.append(",");
                strBuilder.append(nextNext.getValue().getIp());
                strBuilder.append(":");
                strBuilder.append(nextNext.getValue().getPort());
                strBuilder.append(";");
                next = nextNext;
                nextNext = map.higherEntry(next.getKey());
                if (nextNext == null) {
                    nextNext = map.firstEntry();
                }
            }
        }

        strBuilder.append("\r\n");
        return strBuilder.toString();
    }


    public ArrayList<Pair<String, DataMap>> getAll() {
        ArrayList<Pair<String, DataMap>> a = new ArrayList<>();
        map.forEach((s, d) -> a.add(new Pair<>(s, d)));
        return a;
    }

    public void removeEntry(String serverHash) {
        map.remove(serverHash);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }


    public boolean isResponsible(String serverHash, String fileHash) {
        String key = map.ceilingKey(fileHash);
        if (key == null) {
            key = map.firstKey();
        }

        return key.equals(serverHash);
    }
}

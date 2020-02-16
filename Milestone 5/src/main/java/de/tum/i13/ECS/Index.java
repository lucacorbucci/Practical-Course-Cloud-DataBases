package de.tum.i13.ECS;

import de.tum.i13.shared.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import static de.tum.i13.shared.Utility.byteToHex;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class Index {
    private TreeMap<String, DataMap> map = new TreeMap<>(new ComparatorMetadata());

    /**
     * This function returns the successor of a server in the ring
     *
     * @param hash hash of the server
     * @return successor of the server passed as parameter
     */
    synchronized Map.Entry<String, DataMap> getSuccessor(String hash) {
        Map.Entry<String, DataMap> successor = map.higherEntry(hash);
        if (successor == null) {
            successor = map.ceilingEntry(Constants.HEX_START_INDEX);
        }
        return successor;
    }

    /**
     * This function returns the list of all the servers that have joined
     * the network
     *
     * @return List of all the servers
     */
    synchronized ArrayList<Pair<String, Pair<String, Integer>>> getServers() {
        ArrayList<Pair<String, Pair<String, Integer>>> list = new ArrayList<>();
        map.forEach((h, dm) -> list.add(new Pair<>(h, new Pair<String, Integer>(dm.getIp(), dm.getIntraPort()))));
        return list;
    }

    /**
     * This function returns a list of all the servers that are in the network.
     * It is used in the Ping Thread, the only difference wrt getServers is that here
     * we are interested to the ping port and not to the intraport of a server.
     *
     * @return List of all the servers
     */
    synchronized ArrayList<Pair<String, Pair<String, Integer>>> getServersPing() {
        ArrayList<Pair<String, Pair<String, Integer>>> list = new ArrayList<>();
        map.forEach((h, dm) -> list.add(new Pair<>(h, new Pair<String, Integer>(dm.getIp(), dm.getPingPort()))));
        return list;
    }

    /**
     * This function is used when a new server wants to enter in the network, in this
     * case we have to update the metadata to add the new server and to change the
     * datamap of the successor of the new server.
     *
     * @param hashNewServer hash of the server that is joining the network
     * @param ip            ip of the server that is joining the network
     * @param intraPort     intraPort of the server that is joining the network
     * @param port          port of the server that is joining the network
     * @param pingPort      ping port of the server that is joining the network
     * @return true if we add the server false otherwise
     */
    public synchronized boolean addServer(String hashNewServer, String ip, int intraPort, int port, int pingPort) {

        if (map.isEmpty()) {
            BigInteger v = new BigInteger(hashNewServer, 16);
            v = v.add(BigInteger.ONE);
            String start = byteToHex(v.toByteArray());

            map.put(hashNewServer, new DataMap(ip, port, start, hashNewServer, intraPort, pingPort));
            return true;
        } else {
            Map.Entry<String, DataMap> successor = getSuccessor(hashNewServer);

            // Get start value
            DataMap successorDataMap = successor.getValue();
            String newServerStartIndex = successorDataMap.getStartIndex();

            BigInteger value = new BigInteger(hashNewServer, 16);
            value = value.add(BigInteger.ONE);
            String nextHex = byteToHex(value.toByteArray());


            // Update the successor
            map.get(successor.getKey()).setStartIndex(nextHex);
            // Add the new server
            map.putIfAbsent(hashNewServer, new DataMap(ip, port, newServerStartIndex, hashNewServer, intraPort, pingPort));
            return true;
        }
    }

    /**
     * This function returns the number of entry of the index
     *
     * @return the number of entry of the index
     */
    public int size() {
        return map.size();
    }

    /**
     * This function checks if the index contains a specific hash
     *
     * @param hash hash of the server
     * @return true or false
     */
    public boolean contains(String hash) {
        return map.containsKey(hash);
    }

    /**
     * This is useful when a a server sends a shutdown message and the ECS need to
     * delete it from the Metadata. The ECS change also the metadata of the successor of the server
     *
     * @param oldHash hash of the server that we want to remove from the network
     * @return Datamap of the server that we removed
     */
    synchronized DataMap removeServer(String oldHash) {
        DataMap oldServer = map.get(oldHash);
        Map.Entry<String, DataMap> successor = map.higherEntry(oldHash);
        if (successor != null) {
            successor.getValue().setStartIndex(oldServer.getStartIndex());
        } else {
            successor = map.firstEntry();
            if (!successor.getKey().equals(oldHash))
                successor.getValue().setStartIndex(oldServer.getStartIndex());
        }
        return map.remove(oldHash);
    }


    /**
     * This function generates a new Metadata Object that is sent to all
     * the servers when a server go out from the netowrk or when a server enters
     * in the network.
     *
     * @return a new metadata object that will be sent to the others server.
     */
    public synchronized Metadata generateMetadata() {
        return new Metadata(map);
    }

}

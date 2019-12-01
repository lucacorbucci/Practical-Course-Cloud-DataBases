package de.tum.i13.ECS;

import de.tum.i13.shared.Constants;
import de.tum.i13.shared.DataMap;
import de.tum.i13.shared.Metadata;
import de.tum.i13.shared.Pair;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import static de.tum.i13.shared.Utility.byteToHex;
/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class Index {
    private TreeMap<String,DataMap> map = new TreeMap<>();

    synchronized Map.Entry<String, DataMap> getSuccessor(String hash){
        Map.Entry<String, DataMap> successor = map.higherEntry(hash);
        if(successor == null){
            successor = map.ceilingEntry(Constants.HEX_START_INDEX);
        }
        return successor;
    }

    synchronized ArrayList<Pair<String, Pair<String, Integer>>> getServers(){
        ArrayList<Pair<String, Pair<String, Integer>>> list = new ArrayList<>();
        map.forEach((h, dm) -> list.add(new Pair<>(h,new Pair<String, Integer>(dm.getIp(), dm.getIntraPort()))));
        return list;
    }

    synchronized ArrayList<Pair<String, Pair<String, Integer>>> getServersPing(){
        ArrayList<Pair<String, Pair<String, Integer>>> list = new ArrayList<>();
        map.forEach((h, dm) -> list.add(new Pair<>(h,new Pair<String, Integer>(dm.getIp(), dm.getPort()))));
        return list;
    }

    /**
     * This function is used when a new server wants to enter in the network, in this
     * case we have to update the metadata to add the new server and to change the
     * datamap of the successor of the new server.
     *
     *
     */
    public synchronized boolean addServer(String hashNewServer, String ip, int intraPort, int port){

        if(map.isEmpty()){
            BigInteger v = new BigInteger(hashNewServer, 16);
            v = v.add(BigInteger.ONE);
            String start = byteToHex(v.toByteArray());
            map.put(hashNewServer, new DataMap(ip, port, start, hashNewServer, intraPort));
            return true;
        }
        else{
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
            map.putIfAbsent(hashNewServer, new DataMap(ip, port, newServerStartIndex, hashNewServer, intraPort));
            return true;
        }

    }

    public int size(){
        return map.size();
    }

    public boolean contains(String hash){
        return map.containsKey(hash);
    }

    /**
     * This is useful when a a server sends a shutdown message and the ECS need to
     * delete it from the Metadata. The ECS change also the metadata of the successor of the server
     *

     */
    synchronized void removeServer(String oldHash){
        DataMap oldServer = map.get(oldHash);
        Map.Entry<String, DataMap> successor = map.higherEntry(oldHash);
        if(successor != null)
            successor.getValue().setStartIndex(oldServer.getStartIndex());
        map.remove(oldHash);
    }


    /**
     * This function generates a new Metadata Object that is sent to all
     * the servers when a server go out from the netowrk or when a server enters
     * in the network.
     *
     * @return a new metadata object that will be sent to the others server.
     */
    public synchronized Metadata generateMetadata(){
        return new Metadata(map);
    }

}

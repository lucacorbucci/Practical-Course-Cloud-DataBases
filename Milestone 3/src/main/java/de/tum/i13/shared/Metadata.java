package de.tum.i13.shared;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.logging.Logger;

import static de.tum.i13.shared.LogSetup.setupLogging;

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
 *  @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 *
 */
public class Metadata implements Serializable {
    private TreeMap<String,DataMap> map = new TreeMap<>();
    public static Logger logger = Logger.getLogger(Metadata.class.getName());

    public Metadata(TreeMap<String, DataMap> map){
        this.map = map;
    };

    public Metadata(){
        setupLogging( "OFF");
    }

    public Metadata(String s) throws InvalidParameterException{
        setupLogging( "OFF");

        ArrayList<String> data = new ArrayList<>(Arrays.asList(s.split(";")));
        data.forEach((d) -> {
            String[] singleData = d.split(",");
            if(singleData.length != 3) throw new InvalidParameterException();

            String[] ipAndPort = singleData[2].split(":");
            if (ipAndPort.length != 2) throw new InvalidParameterException();

            // String ip, int port, String startIndex, String endIndex, int intraPort
            map.putIfAbsent(singleData[1], new DataMap(ipAndPort[0], Integer.parseInt(ipAndPort[1]),singleData[0], singleData[1], -1));
        });
    }

    public void addAll(Metadata m){
        ArrayList<Pair<String, DataMap>> mm = m.getAll();
        map.clear();
        mm.forEach(p -> map.putIfAbsent(p.getFirst(), p.getSecond()));
    }

    public void print(){
        map.forEach((k,v)-> System.out.println(k));
    }

    public String popFirstKey(){
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
    public Pair<String, Integer> getResponsible(String index) throws NoSuchElementException, NullPointerException{
        if(map == null){
            throw new NoSuchElementException();
        }
        Map.Entry<String, DataMap> m = null;
        try {
            m = map.ceilingEntry(index);
        } catch(NullPointerException e) {
            throw new NullPointerException();
        }

        DataMap dm = (m == null) ? map.firstEntry().getValue(): m.getValue();

        return new Pair<>(dm.getIp(), dm.getPort());
    }

    public int size(){
        return map.size();
    }


    public String getRangeHash(String index) throws NoSuchElementException{
        if(map == null){
            throw new NoSuchElementException();
        }
        Map.Entry<String, DataMap> m = null;
        try {
            m = map.ceilingEntry(index);
        } catch(NullPointerException e) {
            throw new NullPointerException();
        }

        String hash = (m == null) ? map.firstEntry().getKey(): m.getKey();

        return hash;
    }

    /**
     *
     * @return the String with all the data contained in the metadata table
     *         the format of the string is:
     *         StartHash, EndHash, ip:port;
     */
    public String toString(){
        StringBuilder strBuilder = new StringBuilder();
        map.forEach((s,d) ->{
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

    public ArrayList<Pair<String, DataMap>> getAll(){
        ArrayList<Pair<String, DataMap>> a = new ArrayList<>();
        map.forEach((s,d) -> a.add(new Pair<>(s, d)));
        return a;
    }

    public void removeEntry(String serverHash){
        map.remove(serverHash);
    }

    public boolean isEmpty(){
        return map.isEmpty();
    }



    public boolean isResponsible(String serverHash, String fileHash){
        String key = map.ceilingKey(fileHash);
        if(key == null){
            key = map.firstKey();
        }
        logger.info("IsResponsible ORIGINALSERVERHAsh: " +  serverHash);
        logger.info("IsResponsible SERVER: " + key);
        logger.info("IsResponsible FILE HASH: " + fileHash);

        if(key.equals(serverHash))
            return true;
        else return false;
    }

}

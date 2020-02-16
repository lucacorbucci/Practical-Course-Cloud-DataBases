package de.tum.i13.shared;

import java.io.Serializable;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class DataMap implements Serializable {
    private int port;
    private String ip;
    private String startIndex;
    private String endIndex;
    private int intraPort;
    private int pingPort;

    /**
     * Constructor of DataMap
     *
     * @param ip         server's ip
     * @param port       server's port
     * @param startIndex start index of the server
     * @param endIndex   endIndex of the server
     * @param intraPort  intraPort (KVIntraCommunication) port for this server
     */
    public DataMap(String ip, int port, String startIndex, String endIndex, int intraPort) {
        this.ip = ip;
        this.port = port;
        this.startIndex = startIndex;

        this.endIndex = endIndex;
        this.intraPort = intraPort;
    }

    /**
     * Constructor of DataMap
     *
     * @param ip         server's ip
     * @param port       server's port
     * @param startIndex start index of the server
     * @param endIndex   endIndex of the server
     * @param intraPort  intraPort (KVIntraCommunication) port for this server
     * @param pingPort   port for ping
     */
    public DataMap(String ip, int port, String startIndex, String endIndex, int intraPort, int pingPort) {
        this.ip = ip;
        this.port = port;
        this.startIndex = startIndex;
        if (startIndex.length() == 34) {
            this.startIndex = this.startIndex.substring(2);
        }
        this.endIndex = endIndex;
        this.intraPort = intraPort;
        this.pingPort = pingPort;
    }

    /**
     * Getter for the ping port
     *
     * @return The ping port of a specific server
     */
    public int getPingPort() {
        return pingPort;
    }

    /**
     * Getter for the intra port of a server
     *
     * @return The ping port of a specific server
     */
    public int getIntraPort() {
        return intraPort;
    }

    /**
     * Getter for the port of a server
     *
     * @return The ping port of a specific server
     */
    public int getPort() {
        return port;
    }

    /**
     * Getter for the ip of a server
     *
     * @return The ping port of a specific server
     */
    public String getIp() {
        return ip;
    }

    /**
     * Getter for the start index of a server
     *
     * @return The start index of a specific server
     */
    public String getStartIndex() {
        return startIndex;
    }

    /**
     * Getter for the end index
     *
     * @return The endIndex of a specific server
     */
    public String getEndIndex() {
        return endIndex;
    }

    /**
     * This method changes the start index of a server
     */
    public void setStartIndex(String startIndex) {
        this.startIndex = startIndex;
        if (startIndex.length() == 34) {
            this.startIndex = this.startIndex.substring(2);
        }
    }
}

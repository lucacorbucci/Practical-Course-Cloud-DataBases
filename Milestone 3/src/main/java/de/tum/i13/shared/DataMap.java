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

    public DataMap(String ip, int port, String startIndex, String endIndex, int intraPort){
        this.ip = ip;
        this.port = port;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.intraPort = intraPort;
    }

    public int getIntraPort(){
        return intraPort;
    }

    public int getPort() {
        return port;
    }

    public String getIp() {
        return ip;
    }

    public String getStartIndex() {
        return startIndex;
    }

    public String getEndIndex() {
        return endIndex;
    }

    public void setStartIndex(String startIndex) {
        this.startIndex = startIndex;
    }
}

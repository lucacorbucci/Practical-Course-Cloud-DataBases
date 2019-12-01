package de.tum.i13.shared;
/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class ServerStatus {
    private String status;

    public ServerStatus(String status) {
        this.status = status;
    }

    public synchronized void setStatus(String status) {
        this.status = status;
    }

    public synchronized boolean checkEqual(String status){
        if(status.equals(this.status))
            return true;
        else
            return false;
    }
}

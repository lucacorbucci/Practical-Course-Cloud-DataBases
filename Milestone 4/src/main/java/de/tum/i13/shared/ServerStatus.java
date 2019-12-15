package de.tum.i13.shared;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class ServerStatus {
    private String status;

    /**
     * Constructor for the ServerStatus class
     * @param status
     */
    public ServerStatus(String status) {
        this.status = status;
    }

    /**
     * This method is called when we want to change the status of a server
     * @param status the new status for this server
     */
    public synchronized void setStatus(String status) {
        this.status = status;
    }

    /**
     * This method check if the status of a server is equal to the status passed
     * as a parameter
     * @param status status that we want to check
     * @return true if the status is equal, false otherwise
     */
    public synchronized boolean checkEqual(String status) {
        return status.equals(this.status);
    }

    /**
     * Getter for the status of a server
     * @return Status of a server
     */
    public String getStatus() {
        return status;
    }
}

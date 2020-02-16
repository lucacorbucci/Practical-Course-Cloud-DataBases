package de.tum.i13.client;

/**
 * This class is used to encapsulate all the possible client's requests
 *
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public interface KVStoreLibraryInterface {
    /**
     * This method is used to send a get request to the server
     *
     * @param activeConnection the activeConnection that we want to use
     *                         to send the request
     * @param command          the user's request
     */
    void getValue(ActiveConnection activeConnection, String[] command, String... password);

    /**
     * This method is used to send a PUT request or a DELETE request
     * to the server
     *
     * @param activeConnection the activeConnection that we want to use
     *                         to send the request
     * @param command          the user's request
     */
    void putKV(ActiveConnection activeConnection, String[] command);

    /**
     * This method is used to send a KeyRange request to the server.
     * The server will reply with keyrange_success <range_from>,<range_to>,<ip:port>;...
     * <p>
     * In case of failure the server will reply with "server_stopped"
     */
    void keyRange(ActiveConnection activeConnection) throws Exception;

    /**
     * This method is used to send a KeyRange_Read request to the server.
     * The server will reply with keyrange_read_success <range_from>,<range_to>,<ip:port>;...
     * <p>
     * In case of failure the server will reply with "server_stopped"
     */
    void keyRangeRead(ActiveConnection activeConnection) throws Exception;

    /**
     * This method is used to close the connection to the server
     *
     * @param activeConnection
     */
    void closeConnection(ActiveConnection activeConnection);

    /**
     * Open the connection to the server.
     *
     * @param command the list with the request made by the user
     * @return the new open connection
     */
    ActiveConnection buildConnection(String[] command);
}

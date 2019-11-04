package de.tum.i13.client;

/**
 * This class is used to encapsulate all the possible client's requests
 *
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public interface KVStoreLibraryInterface {
    /**
     * This method is used to send a get request to the server
     * @param activeConnection the activeConnection that we want to use
     *                         to send the request
     * @param command the user's request
     */
    void getValue(ActiveConnection activeConnection, String[] command);

    /**
     * This method is used to send a PUT request or a DELETE request
     * to the server
     * @param activeConnection the activeConnection that we want to use
     *                         to send the request
     * @param command the user's request
     */
    void putKV(ActiveConnection activeConnection, String[] command);

    /**
     * This method is used to change the log level.
     * @param command array that contains the user's request
     */
    void changeLogLevel(String[] command);

    /**
     * This is used to print a message with all the instructions to use the client
     */
    void printHelp();

    /**
     * Used to print an error message
     */
    void retUnknownCommand();

    /**
     * Used to print a message
     * @param msg message that we want to print
     */
    void printEchoLine(String msg);

    /**
     * This method is used to close the connection to the server
     *
     * @param activeConnection
     */
    void closeConnection(ActiveConnection activeConnection);

    /**
     * Open the connection to the server.
     * @param command the list with the request made by the user
     * @return the new open connection
     */
    ActiveConnection buildConnection(String[] command);
}

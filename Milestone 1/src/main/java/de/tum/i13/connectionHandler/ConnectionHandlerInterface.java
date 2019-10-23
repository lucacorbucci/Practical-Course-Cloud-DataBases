package de.tum.i13.connectionHandler;


/**
 * ConnectionHandler is the library that we use to handle all the operation
 * that a user is allowed to do.
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public interface ConnectionHandlerInterface {


    /**
     *  This function is used to open a connection to the specified ipAddress
     *  using the specified port.
     *  To connect to the server we use a Java socket.
     * @param   ipAddress   This parameter indicates the ipAddress of the server
     *                      we want to connect
     * @param   port        This is the port where the server is listening
     */
    void open(String ipAddress, int port);

    /**
     *  This function is used to disconnect from the server.
     *  When quit() is called we close the socket, the inputStream and the outputStream
     *
     */
    void quit();

    /**
     *  This function is used to send a message to the server using the socket.
     *  We send an array of bytes to the server
     *
     * @param   buffer The message that we want to send to the server

     */
    int send(String buffer);

    /**
     *  This function reads a fixed amount of bytes received from the server
     *  using the socket.
     *
     * @param   len this parameter allows us to understand how many bytes we have to read
     *              from the socket. It is optional.
     * @return      the message received from the socket
     */
    String receive(int... len);


    /**
     *
     * @return     True if the socket is connected to the server, False if it is not connected
     */
    boolean isConnected();
}

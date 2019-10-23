package de.tum.i13.connectionHandler;

import de.tum.i13.shared.Constants;

import java.io.*;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * ConnectionHandler is the library that we use to handle all the operation
 * that a user is allowed to do.
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class ConnectionHandler implements ConnectionHandlerInterface{
	
	private Socket socket;
	private InputStream in;
	private OutputStream out;
	private Logger LOGGER;

	/**
	 * Creates the ConnectionHandler.
	 *
	 * @param logger This is the logger that we use to log all the operations
	 */
	public ConnectionHandler(Logger logger) {
		this.LOGGER = logger;
	}

	/**
	 *  This function is used to open a connection to the specified ipAddress
	 *  using the specified port.
	 *  To connect to the server we use a Java socket.
	 * @param   ipAddress   This parameter indicates the ipAddress of the server
	 *                      we want to connect
	 * @param   port        This is the port where the server is listening
	 */
	public void open(String ipAddress, int port){
		LOGGER.info("ConnectionHandler.open()");
		try {
			LOGGER.finest("Connecting to server");
			socket = new Socket(ipAddress, port);
			LOGGER.finest("Opening input stream");
			in = socket.getInputStream();
			LOGGER.fine("Opening outputstream");
			out = socket.getOutputStream();
		} catch (NullPointerException e) {
			LOGGER.warning("One of the provided parameter is NULL");
			System.out.println("The provided address is null, please try again with a valid address.");
		} catch (SecurityException e) {
			LOGGER.severe("Not allowed to connect, please try again.");
			System.out.println("Not allowed to connect, please try again.");
		} catch (IllegalArgumentException e) {
			LOGGER.severe("Port not valid");
			System.out.println("The provided port isn't valid, please try a port between 0 and 65535.");
		} catch (IOException e) {
			LOGGER.severe("An error has occured while establishing a connection");
			System.out.println("An error has occured while establishing a connection, check address and port and try again.");
		}
	}

	/**
	 *  This function is used to disconnect from the server.
	 *  When quit() is called we close the socket, the inputStream and the outputStream
	 *
	 */
	public void quit() {
		LOGGER.info("ConnectionHandler.quit()");
		try {
			if(socket != null && socket.isConnected()){
				LOGGER.fine("Closing input streams");
				LOGGER.finest("Closing inputStream");
				in.close();
				LOGGER.finest("Closing outputStream");
				out.close();
				LOGGER.fine("Closing socket");
				socket.close();
			}
		} catch(NullPointerException | IOException e){
			LOGGER.severe("An error has occured while closing the connection");
			System.out.println("An error has occured while closing the connection.");
		}
	}

	/**
	 *  This function is used to send a message to the server using the socket.
	 *  We send an array of bytes to the server
	 *
	 * @param   buffer The message that we want to send to the server

	 */
	public int send(String buffer) {
		LOGGER.info("ConnectionHandler.send()");
		byte[] toSend = null;
		if(buffer.equals("") || !buffer.contains("\r\n")){
			LOGGER.severe("Invalid string");
			System.out.println("Invalid string");
			return -1;
		}
		try {
			toSend = buffer.getBytes();
		} catch (NullPointerException e){
			LOGGER.severe("Invalid string");
			System.out.println("Invalid string");
			return -1;
		}
		int size = toSend.length;
		if(size > Constants.MAX_SIZE){
			LOGGER.warning("The message is not allowed to be bigger than 128 kB");
			System.out.println("Error: The message is not allowed to be bigger than 128 kB");
			size = -1;
		}
		else{
			try {
				LOGGER.fine(String.format("Trying to send to the message: '%s' to the server", buffer));
				out.write(toSend);
				LOGGER.finest("Flushing the buffer");
				out.flush();
				LOGGER.finer("Message sent");
			} catch (NullPointerException | IOException e) {
				LOGGER.severe("An error has occured while sending, please try again.");
				System.out.println("An error has occured while sending, please try again.");
				return -1;
			}
		}
		return size;
	}


	/**
	 *  This function reads a fixed amount of bytes received from the server
	 *  using the socket.
	 *
	 * @param   len this parameter allows us to understand how many bytes we have to read
	 *              from the socket. It is optional.
	 * @return      the message received from the socket, null if an error occurs
	 */
	public String receive(int... len) {
		LOGGER.info("ConnectionHandler.receive()");
		if(len.length != 0 && len[0] < 0){
			return null;
		}

		int nRead, count = 0;
		boolean end = false;
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] data = new byte[Constants.BYTE_ARRAY_SIZE];
		while (!end) {
			try {
				LOGGER.finest("Reading data from socket");
				nRead = this.in.read(data, 0, data.length);
				buffer.write(data, 0, nRead);
				count += nRead;


				if(len.length != 0){
					if (count == len[0])
						end = true;
				}
				else if(this.in.available() <= 0){
					end = true;
				}

			} catch (IOException e) {
				System.out.println("An error has occured while receiving the message.");
			}
		}
		String received = null;
		try {
			data = buffer.toByteArray();
			buffer.close();
			received = new String(data, Constants.TELNET_ENCODING);
			LOGGER.fine(String.format("Received message: '%s' from the server", received));
		} catch (UnsupportedEncodingException e) {
			LOGGER.severe("An error occurred while decoding the received message.");
			System.out.println("An error occurred while decoding the received message");
		} catch (IOException e) {
			LOGGER.severe("An error occurred while decoding the received message.");
			System.out.println("An error occurred while decoding the received message");
		}

		return received;
	}

	/**
	 *
	 * @return  True if the socket is connected to the server, False if it is not connected
	 */
	public boolean isConnected() {
		LOGGER.info("ConnectionHandler.isConnected()");
		return (socket != null && !socket.isClosed());
	}
}

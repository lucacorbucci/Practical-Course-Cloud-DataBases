package de.tum.i13.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.tum.i13.shared.LogSetup.setupLogging;
import de.tum.i13.connectionHandler.ConnectionHandler;
import de.tum.i13.shared.Constants;

/**
 * Client class, we use this to handle the possible requests of the user
 * 
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class Client {
    private final static Logger LOGGER = Logger.getLogger(Client.class.getName());
    static ConnectionHandler connection;
    static String[] requests;
    static String ipAddress;
    static int port;

    /**
     * This function shows the intended usage of the client. If an error occurred it
     * additionally points out the error.
     * 
     * @param error            signalizes if an error occurred or not
     * @param errorDescription stores the errors that occurred
     */
    private static void printHelp(boolean error, String... errorDescription) {
        if (error) {
            LOGGER.warning("Not connected");
            // Using the constant ANSI_RED we can print the error messages in red
            System.out.println(Constants.ANSI_RED + errorDescription[0] + Constants.ANSI_RESET);
        }
        System.out.println("EchoClient: to use this client you must write one of the following commands:\n"
                + "- connect <IP_ADDRESS> <PORT> : to connect to the server\n"
                + "- disconnect : to disconnect from the server\n"
                + "- send <message> : to send a message to the server\n"
                + "- logLevel <level> : to change the level of logging\n"
                + "- quit : to close all the active connections and quit the client\n");
    }

    /**
     * This function merges a string array into a single string. The strings are
     * separated through a blank space. The end of the string is visible through the
     * manually added return carriage(\r\n)
     * 
     * @param requests this parameter contains the message the user wants to send to
     *                 the client starting from index 1
     * @return a single string which contains the message the user wants to send to
     *         the client
     */
    private static String createMessage(String[] requests) {
        StringBuilder message = new StringBuilder();

        for (int i = 1; i < requests.length; i++) {
            if (i == requests.length - 1) {
                message.append(requests[i]);
                message.append("\r\n");
            } else {
                message.append(requests[i]);
                message.append(" ");
            }
        }
        return message.toString();
    }

    public static void main(String[] args) {

        BufferedReader prompt = new BufferedReader(new InputStreamReader(System.in));
        boolean quit = false;
        connection = new ConnectionHandler(LOGGER);
        setupLogging("test.log", LOGGER);

        // Using this addShutdownHook we can catch SIGINT and also we can terminate the
        // client when the user sends the command quit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n\nClosing all active connections... \n Goodbye!");
            connection.quit();
        }));

        while (!quit) {
            System.out.print("EchoClient>");
            try {
                String input = prompt.readLine();
                requests = input.trim().split("\\s+");

                switch (requests[0]) {
                case "connect": {
                    connect(connection);
                    break;
                }
                case "disconnect": {
                    disconnect(connection);
                    break;
                }
                case "send": {
                    send();
                    break;
                }
                case "logLevel": {
                    setLogLevel();
                    break;
                }
                case "help": {
                    printHelp(false);
                    break;
                }
                case "quit": {
                    quit = true;
                    break;
                }
                default: {
                    printHelp(true, "Error: Unknown command, please use one of the following commands:");
                    break;
                }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    private static void connect(ConnectionHandler connection){
        if (requests.length == 3) {
            ipAddress = requests[1];
            port = Integer.parseInt(requests[2]);
            connection.open(ipAddress, port);
            if (connection.isConnected())
                System.out.print(connection.receive());
        } else {
            printHelp(true, "Error: You have not specified all the required parameters");
        }
    }

    private static void disconnect(ConnectionHandler connection){
        if (connection.isConnected()) {
            connection.quit();
            System.out.printf("Connection terminated %s:%s\n", ipAddress, port);

        } else {
            printHelp(true, "Error: Not connected yet.");
        }
    }

    private static void send(){
        if(requests.length < 2){
            printHelp(true, "Error: You have not specified all the required parameters");
        }
        if (connection.isConnected()) {
            String toSend = createMessage(requests);
            int size = connection.send(toSend);

            if (size > 0) // In this case no error while sending the data, so we want to read the echo
                // message
                System.out.print(connection.receive(size));
        } else {
            printHelp(true, "Error: Not connected yet.\n");
        }
    }

    private static void setLogLevel(){
        if (requests.length == 2) {
            String oldLevel = LOGGER.getLevel().toString();
            switch (requests[1]) {
                case "ALL": {
                    LOGGER.setLevel(Level.ALL);
                    System.out.printf("LogLevel set from %s to %s\n", oldLevel, LOGGER.getLevel().toString());
                    break;
                }
                case "INFO": {
                    LOGGER.setLevel(Level.INFO);
                    System.out.printf("LogLevel set from %s to %s\n", oldLevel, LOGGER.getLevel().toString());
                    break;
                }
                case "WARNING": {
                    LOGGER.setLevel(Level.WARNING);
                    System.out.printf("LogLevel set from %s to %s\n", oldLevel, LOGGER.getLevel().toString());
                    break;
                }
                case "CONFIG": {
                    LOGGER.setLevel(Level.CONFIG);
                    System.out.printf("LogLevel set from %s to %s\n", oldLevel, LOGGER.getLevel().toString());
                    break;
                }
                case "FINE": {
                    LOGGER.setLevel(Level.FINE);
                    System.out.printf("LogLevel set from %s to %s\n", oldLevel, LOGGER.getLevel().toString());
                    break;
                }
                case "FINER": {
                    LOGGER.setLevel(Level.FINER);
                    System.out.printf("LogLevel set from %s to %s\n", oldLevel, LOGGER.getLevel().toString());
                    break;
                }
                case "FINEST": {
                    LOGGER.setLevel(Level.FINEST);
                    System.out.printf("LogLevel set from %s to %s\n", oldLevel, LOGGER.getLevel().toString());
                    break;
                }
                case "SEVERE": {
                    LOGGER.setLevel(Level.SEVERE);
                    System.out.printf("LogLevel set from %s to %s\n", oldLevel, LOGGER.getLevel().toString());
                    break;
                }
                case "OFF": {
                    LOGGER.setLevel(Level.OFF);
                    System.out.printf("LogLevel set from %s to %s\n", oldLevel, LOGGER.getLevel().toString());
                    break;
                }
                default: {
                    printHelp(true, "Error: You have not specified a valid level for Logging");
                    break;
                }
            }
        } else {
            printHelp(true, "Error: You have not specified all the required parameters");
        }
    }
}

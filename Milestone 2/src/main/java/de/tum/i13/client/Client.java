package de.tum.i13.client;

import de.tum.i13.shared.LogSetup;
import de.tum.i13.shared.LogeLevelChange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to launch the server and to handle all the user's
 * requests.
 *
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class Client {

    public static Logger logger = Logger.getLogger(Client.class.getName());
    ActiveConnection activeConnection = null;
    KVStoreLibrary kvs = null;

    public static void main(String[] args) throws IOException {
        Client mm = new Client();

        LogSetup.changeLoglevel(Level.OFF);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Closing Client");
            mm.close();
        }));
        mm.start();
    }

    /**
     * This method is used to handle all the possible user's requests
     * @throws IOException
     */
    private void start() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        kvs = new KVStoreLibrary(logger);
        for(;;) {
            System.out.print("EchoClient> ");
            String line = reader.readLine();
            logger.finest(line);
            if(!line.isEmpty()) {
                String[] command = line.split(" ");
                logger.fine(String.format("command: %s", command));
                switch (command[0]) {
                    case "connect":
                        activeConnection = kvs.buildConnection(command);
                        break;
                    case "put":
                        kvs.putKV(activeConnection, command);
                        break;
                    case "get":
                        kvs.getValue(activeConnection, command);
                        break;
                    case "disconnect":
                        kvs.closeConnection(activeConnection);
                        break;
                    case "loglevel":
                        kvs.changeLogLevel(command);
                        break;
                    case "help":
                        kvs.printHelp();
                        break;
                    case "quit":
                        kvs.printEchoLine("Application exit!");
                        return;
                    default:
                        kvs.retUnknownCommand();
                }
            }
        }
    }

    /**
     * This method is used to close the connection to the server
     */
    private void close() {
        if(activeConnection != null) {
            try {
                activeConnection.close();
            } catch (Exception e) {
                //Not much we can do
            }
        }
    }

}

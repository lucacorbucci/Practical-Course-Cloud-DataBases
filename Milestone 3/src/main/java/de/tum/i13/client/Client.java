package de.tum.i13.client;

import de.tum.i13.shared.LogSetup;
import de.tum.i13.shared.LogeLevelChange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.tum.i13.shared.LogSetup.setupLogging;
import static de.tum.i13.shared.Utility.printEchoLine;
import static de.tum.i13.shared.Utility.retUnknownCommand;

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
        Path path = Paths.get("client.log");
        setupLogging(path, "OFF");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Closing Client");
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
                        changeLogLevel(command);
                        break;
                    case "help":
                        printHelp();
                        break;
                    case "quit":
                        printEchoLine("Application exit!");
                        return;
                    default:
                        retUnknownCommand();
                }
            }
        }
    }



    /**
     * This method is used to change the log level.
     * @param command array that contains the user's request
     */
    public void changeLogLevel(String[] command) {
        if(command.length == 1){
            Level l = logger.getLevel();
            if(l == null)
                printEchoLine(String.format("Current loglevel OFF"));
            else
                printEchoLine(String.format("Current loglevel %s", logger.getLevel()));
        }
        else{
            try {
                Level level = Level.parse(command[1]);
                LogeLevelChange logeLevelChange = LogSetup.changeLoglevel(level);
                printEchoLine(String.format("loglevel changed from: %s to: %s", logeLevelChange.getPreviousLevel(), logeLevelChange.getNewLevel()));
            } catch (IllegalArgumentException ex) {
                printEchoLine("Unknown loglevel");
            }
        }
    }

    /**
     * This is used to print a message with all the instructions to use the client
     */
    public void printHelp() {
        System.out.println("Available commands:");
        System.out.println("connect <address> <port> - Tries to establish a TCP- connection to the echo server based on the given server address and the port number of the echo service.");
        System.out.println("disconnect - Tries to disconnect from the connected server.");
        System.out.println("get <key> - Requests the value of the given key from the database server.");
        System.out.println("put <key> <value> - Stores the key value pair on the database server.");
        System.out.println("put <key> - Deletes the value of the given key from the database server.");
        System.out.println(String.format("logLevel <level> - Sets the logger to the specified log level (%s | DEBUG | INFO | WARN | ERROR | FATAL | OFF)", Level.ALL.getName()));
        System.out.println("help - Display this help");
        System.out.println("quit - Tears down the active connection to the server and exits the program execution.");
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

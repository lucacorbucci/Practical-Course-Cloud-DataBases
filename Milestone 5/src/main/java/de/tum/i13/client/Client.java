package de.tum.i13.client;

import de.tum.i13.shared.LogSetup;
import de.tum.i13.shared.LogeLevelChange;
import de.tum.i13.shared.inputPassword;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
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

    private ActiveConnection activeConnection = null;
    private KVStoreLibrary kvs = null;
    private static FileHandler fileHandler;
    private de.tum.i13.shared.inputPassword inputPassword = new inputPassword(false, 0);

    public static void main(String[] args) throws IOException {
        Client mm = new Client();
        Path path = Paths.get("client.log");
        fileHandler = setupLogging(path, "ALL", logger);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Closing Client");
            fileHandler.close();
            mm.close();
        }));
        mm.start();
    }

    /**
     * This method is used to handle all the possible user's requests
     *
     * @throws IOException
     */
    private void start() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        kvs = new KVStoreLibrary(logger, inputPassword);



        for (; ; ) {
            System.out.print("EchoClient> ");
            String line = reader.readLine();
            logger.finest(line);
            if (!line.isEmpty()) {
                String[] command = line.split(" ");
                logger.fine(String.format("command: %s", command));
                if (!checkInput(command)) {
                    switch (command[0]) {
                        case "connect":
                            activeConnection = kvs.buildConnection(command);
                            break;
                        case "put":
                            inputPassword.setPrevCommand(command);
                            kvs.putKV(activeConnection, command);
                            break;
                        case "putWithPassword":
                            handlePutWithPassword(command);
                            break;
                        case "get":
                            inputPassword.setPrevCommand(command);
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
    }

    /**
     * This method is used to handle the put request with password
     *
     * @param command array that contains the user's request
     */
    private void handlePutWithPassword(String[] command) {
        if (command.length >= 1) {
            setInput(command);
            printEchoLine("Input Password: ");
        } else
            retUnknownCommand();
    }

    /**
     * This method is used to handle the requests made by the user when the user
     * has to insert the password to access the key.
     * We count how many times the user try to access the key with the wrong password
     * and if the user tries more than 3 times we stop to request the password and we stop the
     * request to the server.
     * @param command command sent by the user
     * @return true if the user has to insert password, false otherwise
     */
    private boolean checkInput(String[] command) {
        logger.info("isInputPassword " +  inputPassword.isInputPassword());
        logger.info("PrevCommand " + inputPassword.getPrevCommand().length);
        logger.info("Counter " + inputPassword.getCountPasswordInput());
        logger.info(" " + inputPassword.getPrevCommand().toString());
        if (inputPassword.getPrevCommand().length != 0 && inputPassword.isInputPassword() && inputPassword.getCountPasswordInput() <= 2) {
            inputPassword.increaseCounter();
            if (inputPassword.getPrevCommand()[0].equals("get")) {
                kvs.getValue(activeConnection, inputPassword.getPrevCommand(), command);
            } else if (inputPassword.getPrevCommand()[0].equals("putWithPassword") || inputPassword.getPrevCommand()[0].equals("put")) {
                kvs.putKVWithPassword(activeConnection, inputPassword.getPrevCommand(), command);
            }

            if (inputPassword.getCountPasswordInput() == 2) {
                clearInput();
            }
            return true;
        }
        return false;

    }

    /**
     * This method is called to stop the request of the password to the user
     */
    private void clearInput() {
        this.inputPassword.setInputPassword(false);
        this.inputPassword.setCountPasswordInput(0);
        this.inputPassword.clearPrevCommand();
    }

    /**
     * This method is called to start the request of the password to the user
     */
    private void setInput(String[] command) {
        this.inputPassword.setPrevCommand(command);
        this.inputPassword.setCountPasswordInput(0);
        this.inputPassword.setInputPassword(true);
    }


    /**
     * This method is used to change the log level.
     *
     * @param command array that contains the user's request
     */
    public void changeLogLevel(String[] command) {
        if (command.length == 1) {
            Level l = logger.getLevel();
            if (l == null)
                printEchoLine(String.format("Current loglevel OFF"));
            else
                printEchoLine(String.format("Current loglevel %s", logger.getLevel()));
        } else {
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
        if (activeConnection != null) {
            try {
                activeConnection.close();
            } catch (Exception e) {
                //Not much we can do
            }
        }
    }

}

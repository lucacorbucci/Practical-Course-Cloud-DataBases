package de.tum.i13.client;

import de.tum.i13.shared.Constants;
import de.tum.i13.shared.LogSetup;
import de.tum.i13.shared.LogeLevelChange;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to encapsulate all the possible client's requests
 *
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class KVStoreLibrary implements KVStoreLibraryInterface {

    public static Logger logger;

    public KVStoreLibrary(Logger logger){
        this.logger = logger;

    }

    /**
     * Used to print an error message
     */
    public void retUnknownCommand() {
        printEchoLine("Unknown command");
    }

    /**
     * Used to print a message
     * @param msg message that we want to print
     */
    public void printEchoLine(String msg) {
        System.out.print("EchoClient> " + msg + "\n");
    }


    /**
     * This method is used to close the connection to the server
     *
     * @param activeConnection
     */
    public void closeConnection(ActiveConnection activeConnection) {
        logger.info("Connection closed.");
        if(activeConnection != null) {
            try {
                activeConnection.close();
            }
            catch(IOException e){
                activeConnection = null;
            } catch(Exception e) {
                activeConnection = null;
            }
        }
    }



    /**
     * Open the connection to the server.
     * @param command the list with the request made by the user
     * @return the new open connection
     */
    public ActiveConnection buildConnection(String[] command) {
        if(command.length == 3){
            try {
                var kvcb = new ClientConnectionBuilder(command[1], Integer.parseInt(command[2]));
                logger.info("begin connecting");
                ActiveConnection ac = kvcb.connect();
                logger.info("connected");
                String confirmation = ac.readline();
                printEchoLine(confirmation);
                return ac;
            } catch (UnknownHostException e) {
                logger.severe("Not valid IP address");
                printEchoLine("Could not connect to server, ip is not valid");
            } catch (IllegalArgumentException e){
                logger.severe("Not valid port");
                printEchoLine("Could not connect to server, port number is not valid");
            }
            catch (IOException e) {
                logger.severe("Problem with response");
                printEchoLine("An error occurred during connection to the server");
            }
        }
        return null;
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

    private Boolean checkConnection(ActiveConnection activeConnection){
        if(activeConnection == null){
            printEchoLine("Error! Not connected!");
            return false;
        }
        return true;
    }

    private void readResponse(ActiveConnection activeConnection){
        try {
            //reading the response from the server
            String r = activeConnection.readline();
            String[] response = r.split(" ");
            //if the response was successful
            if(response[0].equals("get_success")){
                //print it on the console
                printEchoLine(response[0] + " " + response[1] + " " + decode(response[2]));
                logger.fine("GET received " + response[1] + " " + decode(response[2]));
            } else if(response[0].equals("put_error")) {
                printEchoLine(response[0] + " " + response[1] + " " + decode(response[3].substring(0, response[3].length() - 2)));
            } else {
                //print the message on the console
                printEchoLine(r);
                logger.fine("Received: " + r);
            }

        } catch (IOException e) {
            printEchoLine("Error! Not connected!");
        } catch (Exception e) {
            printEchoLine("Error! Response could not be processed");
        }
    }

    private void sendRequest(ActiveConnection activeConnection, String message){
        try{
            activeConnection.write(message);
            logger.fine(message);
        } catch(Exception e){
            printEchoLine("An error occurred");
            return;
        }
    }

    /**
     * This function is used to check a string is less than len bytes
     * @param string the key that we want to check
     * @param len    the len we want to check
     * @return true if it is <= than len bytes else false
     */
    private boolean isLessThan(String string, int len){
        return string.getBytes().length <= len;
    }

    private String buildValue(String[] command){
        //Build the value parameter
        StringBuilder value = new StringBuilder();
        value.append(command[2]);
        for(int i = 3; i < command.length; i++) {
            value.append(" ");
            value.append(command[i]);
        }
        return value.toString();
    }

    /**
     * This method is used to send a get request to the server
     * @param activeConnection the activeConnection that we want to use
     *                         to send the request
     * @param command the user's request
     */
    public void getValue(ActiveConnection activeConnection, String[] command){
        //if the user input does not have 2 commands or the length of the key is longer then 20 characters
        if(command.length != 2) {
            retUnknownCommand();//user used an unknown command
            return;
        }
        else if(!isLessThan(command[1], Constants.KEY_MAX_LENGTH)){
            printEchoLine(String.format("key must be less than %s bytes", Constants.KEY_MAX_LENGTH));
            return;
        }
        if(!checkConnection(activeConnection)) return;

        //send the command "get key"
        sendRequest(activeConnection, Constants.GET_COMMAND + command[1]);

        //process the response
        readResponse(activeConnection);
    }

    /**
     * This method is used to send a PUT request or a DELETE request
     * to the server.
     * @param activeConnection the activeConnection that we want to use
     *                         to send the request
     * @param command the user's request
     */
    public void putKV(ActiveConnection activeConnection, String[] command){
        //if the user input is less then 2 commands
        if(command.length < 2) {
            //return unknown command
            retUnknownCommand();
            return;
        }
        if(!checkConnection(activeConnection)) return;
        Boolean ret = false;
        //Differentiate between PUT and DELETE
        //if there are exactly 2 commands we want to delete the pair
        if(command.length == 2) {
            ret = handleDeleteRequest(activeConnection, command);
        }
        else {
            ret = handlePutRequest(activeConnection, command);
        }
        if(ret)
            readResponse(activeConnection);
    }

    /**
     * This function is used to send a delete request
     * @param activeConnection the active connection that we want to use
     * @param command the command that we want to process
     */
    private boolean handleDeleteRequest(ActiveConnection activeConnection, String[] command){
        if(isLessThan(command[1], Constants.KEY_MAX_LENGTH)) {
            sendRequest(activeConnection, Constants.DELETE + command[1]);
            logger.fine("DELETE sent " + command[1]);
            return true;
        } else {
            printEchoLine(String.format("key must be less than %s bytes", Constants.KEY_MAX_LENGTH));
            return false;
        }
    }

    /**
     * This function is used to send a put request
     * @param activeConnection the active connection that we want to use
     * @param command the command that we want to process
     */
    private boolean handlePutRequest(ActiveConnection activeConnection, String[] command){
        String value = buildValue(command);
        if(isLessThan(value, Constants.VALUE_MAX_LENGTH) && isLessThan(command[1] ,Constants.KEY_MAX_LENGTH)) {
            sendRequest(activeConnection, Constants.PUT + command[1] + " " + encode(value));
            logger.fine("PUT sent " + command[1] + " " + value);
            return true;
        } else {
            printEchoLine(String.format("key must be less than %s bytes, value less than %s", Constants.KEY_MAX_LENGTH, Constants.VALUE_MAX_LENGTH));
            return false;
        }
    }

    /**
     * This is used to decode a received message
     * @param string the encoded string
     * @return the decoded string
     */
    private static String decode(String string) {
        byte[] a = hexToByte(string);
        return new String(a);
    }

    /**
     * This is used to encode a plain text string to HEX String
     * We use this method do encode the value to delete the \r\n so that
     * we can send it to the server without any problem
     * @param string the string that we want to encode
     * @return the encoded string
     */
    private static String encode(String string) {
        byte[] byteArray = string.getBytes();
        return byteToHex(byteArray);
    }

    /**
     * This is used to convert an HEX string to byte array
     * @param hex string to be converted
     * @return byte array with the converted string
     */
    private static byte[] hexToByte(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            int j = Integer.parseInt(hex.substring(index, index + 2), 16);
            bytes[i] = (byte) j;
        }
        return bytes;
    }

    /**
     * This is used to convert a byte array to hex Strig
     * @param in byte array to be converted
     * @return encoded string
     */
    private static String byteToHex(byte[] in) {
        StringBuilder sb = new StringBuilder();
        for(byte b : in) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

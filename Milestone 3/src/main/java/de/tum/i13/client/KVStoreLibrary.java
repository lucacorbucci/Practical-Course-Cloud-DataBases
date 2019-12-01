package de.tum.i13.client;

import de.tum.i13.shared.Constants;
import de.tum.i13.shared.DataMap;
import de.tum.i13.shared.Metadata;
import de.tum.i13.shared.Pair;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.tum.i13.shared.Constants.HEX_END_INDEX;
import static de.tum.i13.shared.Constants.HEX_START_INDEX;
import static de.tum.i13.shared.LogSetup.setupLogging;
import static de.tum.i13.shared.Utility.printEchoLine;
import static de.tum.i13.shared.Utility.retUnknownCommand;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * This class is used to encapsulate all the possible client's requests
 *
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class KVStoreLibrary implements KVStoreLibraryInterface {

    private Metadata metadata;
    public static Logger logger = Logger.getLogger(KVStoreLibrary.class.getName());
    public KVStoreLibrary(Logger logs){
        setupLogging("OFF");
    }

    /**
     * Open the connection to the server.
     * @param command the list with the request made by the user
     * @return the new open connection
     */
    public ActiveConnection buildConnection(String[] command) {

        if(command.length == 3){
            try {
                logger.info("Request " + command[1] + " " + command[2]);
                ClientConnectionBuilder kvcb = new ClientConnectionBuilder(command[1], Integer.parseInt(command[2]));
                

                DataMap dataMap = new DataMap(command[1], Integer.parseInt(command[2]), HEX_START_INDEX, HEX_END_INDEX,-1);
                TreeMap<String, DataMap> treeMap = new TreeMap<String, DataMap>();
                treeMap.put(HEX_START_INDEX, dataMap);
                metadata = new Metadata(treeMap);
                    
                logger.info("begin connecting " + command[1] + " " +  Integer.parseInt(command[2]));
                
                ActiveConnection ac = kvcb.connect();

                logger.info("connected");
                String confirmation = ac.readline();
                printEchoLine(confirmation);
                return ac;
            } catch(NullPointerException e){
                logger.info("ciao");
            }
            catch (UnknownHostException e) {
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
        else{
            printHelp();
        }
        return null;
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
        //Differentiate between PUT and DELETE
        //if there are exactly 2 commands we want to delete the pair
        if(command.length == 2) {
            handleDeleteRequest(activeConnection, command);
        }
        else {
            handlePutRequest(activeConnection, command);
        }
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

        handleMessage(activeConnection, Constants.GET_COMMAND + command[1], command[1]);
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


    /****************************************************************/





    private int readResponse(ActiveConnection activeConnection){
        try {
            //reading the response from the server
            logger.info("Reading...");

            String r = activeConnection.readline();
            logger.info("Read " + r);
            String[] response = r.split(" ");
            //if the response was successful
            if(response[0].equals("get_success")){
                System.out.flush();
                StringBuilder str = new StringBuilder();
                str.append(response[0]);
                str.append(" ");
                str.append(response[1]);
                str.append(" ");
                str.append(decode(response[2]));
                //print it on the console
                printEchoLine(str.toString());
                logger.fine("GET received " + response[1] + " " + decode(response[2]));
            } else if(response[0].equals("put_error")) {
                printEchoLine(response[0] + " " + response[1] + " " + decode(response[3].substring(0, response[3].length() - 2)));
            } else if(response[0].equals("server_stopped") || response[0].equals("server_write_lock")) {
                return 1;
            } else if(response[0].equals("server_not_responsible")){
                return 2;
            } else{
                //print the message on the console
                printEchoLine(r);
                logger.fine("Received: " + r);
            }
        } catch (IOException e) {
            printEchoLine("Error! Not connected!");
        } catch (Exception e) {
            printEchoLine("Error! Response could not be processed");
        }
        return 0;
    }


    public String getMetadata() {
        return metadata.toString();
    }

    /**
     * This methods handles retrying queries and sending them to the right server
     * @param activeConnection the connection that shall be used
     * @param message the complete message that shall be sent
     * @param key the key sent in the message, used for determining the right server
     */
    private void handleMessage(ActiveConnection activeConnection, String message, String key) {

        // Test doesn't works without the if
       //if(activeConnection == null){
            try {
                chooseServer(activeConnection, key);
            } catch (NoSuchAlgorithmException e) {
                logger.warning("Error. The hashing algorithm is invalid.");
                return;
            } catch (NullPointerException e) {
                logger.warning("All known servers shut down.");
                return;
            } catch (IOException e){
                logger.warning("Error while retrieving metadata from another server.");
            }
        //}
        int attempts = 0;

        while(true) {

            if (!checkConnection(activeConnection)) return;

            //send the request(GET/DELETE/PUT)
            sendRequest(activeConnection,  message);

            //process the response
            switch(readResponse(activeConnection)){
                //Retry sending with backoff

                case 1 :
                    try {
                        MILLISECONDS.sleep((int) (Math.random() * Math.min(1024, Math.pow(2, attempts++))));
                    } catch (InterruptedException e) {
                        logger.warning("Error while retrying to send message");
                    }
                    break;
                //Get the responsible Server
                case 2 :
                    try {
                        keyRange(activeConnection);
                        handleMessage(activeConnection, message, key);
                    } catch (Exception e){
                        logger.warning("keyrange request failed");
                    }
                    return;
                default:
                    return;
            }
        }
    }

    /**
     * This method chooses the right server to send the query to
     * @param activeConnection the connection which shall be used
     * @param key the key that shall be sent
     * @throws NoSuchAlgorithmException
     */
    private void chooseServer(ActiveConnection activeConnection, String key) throws NullPointerException, NoSuchAlgorithmException, IOException {


        // TESTING

       /*if(metadata = null){
            DataMap dataMap = new DataMap(activeConnection.getIp(), activeConnection.getPort(), HEX_START_INDEX, HEX_END_INDEX,-1);
            TreeMap<String, DataMap> treeMap = new TreeMap<String, DataMap>();
            treeMap.put(HEX_START_INDEX, dataMap);
            metadata = new Metadata(treeMap);
        }*/

        if(metadata != null){

            Pair<String, Integer> responsibleServer = metadata.getResponsible(keyHash(key));
            if(!activeConnection.getIp().equals(responsibleServer.getFirst()) || activeConnection.getPort() != responsibleServer.getSecond()) {
                try {
                    activeConnection.reconnect(responsibleServer.getFirst(), responsibleServer.getSecond());
                    activeConnection.readline();
                } catch (IOException e) {
                    logger.warning("Could not connect to responsible Server. Trying to update metadata on another server");
                    metadata.removeEntry(metadata.getRangeHash(keyHash(key)));
                    if(!metadata.isEmpty()) {
                        responsibleServer = metadata.getResponsible(keyHash(key));
                        try {
                            activeConnection.reconnect(responsibleServer.getFirst(), responsibleServer.getSecond());
                            activeConnection.readline();
                            keyRange(activeConnection);

                        } catch (IOException ex) {
                            chooseServer(activeConnection, key);
                        } catch (Exception ex) {
                            throw new IOException();
                        }
                    } else{
                        throw new NullPointerException();
                    }
                }
        }

        }
    }


    /**
     * This method updates the metadata
     * @param activeConnection the connection to retrieve the data from
     * @throws Exception
     */
    @Override
    public void keyRange(ActiveConnection activeConnection) throws Exception {
        int attempts = 0;
        while(true) {

            if (!checkConnection(activeConnection)) return;

            //send the keyrange request
            sendRequest(activeConnection,  "keyrange");
            try {
                String r = activeConnection.readline();
                String[] response = r.split(" ");

                //process the response
                switch(response[0]){
                    //Retry sending with backoff
                    case "server_stopped" :
                        try {
                            MILLISECONDS.sleep((int) (Math.random() * Math.min(1024, Math.pow(2, attempts++))));
                        } catch (InterruptedException e) {
                            logger.warning("Error while retrying to send keyrange request");
                        }
                        break;
                    case "keyrange_success" :
                        if(response.length == 2) {
                            logger.info("Update Metadata");
                            metadata = new Metadata(response[1]);
                            return;
                        }
                    default:
                        throw new Exception();
                }
            } catch (Exception e){
                logger.warning("Error while receiving keyrange answer.");
            }

        }
    }



    /********************************************************************************/

    /**
     * This function is used to send a delete request
     * @param activeConnection the active connection that we want to use
     * @param command the command that we want to process
     */
    private boolean handleDeleteRequest(ActiveConnection activeConnection, String[] command){
        if(isLessThan(command[1], Constants.KEY_MAX_LENGTH)) {
            handleMessage(activeConnection, Constants.DELETE + command[1], command[1]);
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
            handleMessage(activeConnection, Constants.PUT + command[1] + " " + encode(value), command[1]);
            logger.info("PUT sent " + command[1] + " " + value);
            return true;
        } else {
            printEchoLine(String.format("key must be less than %s bytes, value less than %s", Constants.KEY_MAX_LENGTH, Constants.VALUE_MAX_LENGTH));
            return false;
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


    private Boolean checkConnection(ActiveConnection activeConnection){
        if(activeConnection == null){
            printEchoLine("Error! Not connected!!!");
            return false;
        }
        return true;
    }


    private void sendRequest(ActiveConnection activeConnection, String message){
        try{
            activeConnection.write(message);
            logger.fine("MESSAGE SENT: " + message);
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
     * This is used to decode a received message
     * @param string the encoded string
     * @return the decoded string
     */
    private static String decode(String string) {
        byte[] a = hexToByte(string);
        return new String(a);
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

    /**
     * This is used to hash the key via MD5 algorithm
     * @param key string to be hashed
     * @return the hash representation
     * @throws NoSuchAlgorithmException
     */
    private String keyHash(String key) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        messageDigest.update(key.getBytes());
        byte[] byteArr = messageDigest.digest();
        messageDigest.reset();
        return byteToHex(byteArr);
    }
}

package de.tum.i13.ECS;
import de.tum.i13.server.kv.KVIntraCommunication;
import de.tum.i13.shared.DataMap;
import de.tum.i13.shared.LogSetup;
import de.tum.i13.shared.Metadata;
import de.tum.i13.shared.Pair;
import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.tum.i13.shared.LogSetup.setupLogging;
import static de.tum.i13.shared.Utility.byteToHex;
/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class ECS implements ECSInterface, Runnable {
    private Index index = new Index();
    public static Logger logger = Logger.getLogger(ECS.class.getName());
    private ServerSocket serverSocket;
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;
    private Socket socket = null;
    private Thread pinger = null;
    int ecsPort;
    boolean shutdown = false;
    boolean ready = false;

    public ECS(int port, String logging) {
        this.ecsPort = port;
        setupLogging(logging);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("SHUTDOWN HOOK");
            shutdown = true;
            try {
                if(serverSocket != null)
                    serverSocket.close();
                closeAll(in, out, socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }

    /**
     * Test function used in the tests to get the metadata from the ECS
     * @return metadata from the ECS
     */
    public Metadata getMetadata(){
        return index.generateMetadata();
    }

    public void run() {
        initECS();
        startPinger();
        ready = true;
        handleRequests();
    }

    /**
     * This function is used in the tests to check if the ECS is ready to receive requests
     * @return true or false
     */
    public boolean isReady(){
        return this.ready;
    }

    /**
     * This function is used to handle all the requests by the ECS
     */
    private void handleRequests(){
        while(true){
            try {
                if(!shutdown){
                    logger.info("Accepting new connections....");
                    String remoteIP = acceptConnection();
                    String[] request = read(in);
                    if(request!= null){
                        logger.info("Request received: " + request[0]);
                        switch (request[0]){
                            case("newServer"):
                                addNewServer(request, remoteIP, in, out);
                                break;
                            case("shutdownServer"):
                                shutdownServer(request, remoteIP, in, out);
                                break;
                            default:
                                break;
                        }
                    }
                }
                closeAll(in, out, socket);

            } catch (SocketException e) {
                logger.severe("Connection closed");
                if(!shutdown){
                    System.exit(-1);
                }
            } catch (RemoteException e) {
                logger.severe("Error during communication");
                if(!shutdown){
                    System.exit(-1);
                }
            } catch (IOException | NullPointerException e) {
                logger.severe("Error during connection");
                if(!shutdown){
                    System.exit(-1);
                }
            }
        }
    }

    /**
     * This is used to close all the streams and the socket
     * @param in
     * @param out
     * @param socket
     */
    private void closeAll(ObjectInputStream in, ObjectOutputStream out, Socket socket) throws IOException {
        if(in != null){
            in.close();
            in = null;
        }
        if(out != null){
            out.close();
            out = null;
        }
        if(socket != null){
            if(socket.isConnected())
                socket.close();
        }
    }

    /**
     * This function accepts the connestions from the servers
     * @return
     * @throws IOException
     */
    private String acceptConnection() throws IOException {
        socket = serverSocket.accept();
        String remoteIP = socket.getInetAddress().getHostAddress();
        out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        out.flush();
        in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
        return remoteIP;
    }

    /**
     * This function starts the pinger thread
     */
    private void startPinger(){
        pinger = new Thread(new PingThread(index));
        pinger.start();
    }

    /**
     * This function reads all the data from the ObjectInputStream passed as parameter
     * @param remoteIn The ObjectInputStream from which we want to read
     * @return Data that we have read
     */
    private String[] read(ObjectInputStream remoteIn){
        Long toRead;
        String command;
        String[] request=null;
        try {
            if(!shutdown && remoteIn != null){
                toRead = remoteIn.readLong();
                do{
                    command = remoteIn.readUTF();
                } while(command.length() < toRead);
                request = command.split(" ");
            }

        } catch(IOException e){
            if(!shutdown){
                System.exit(-1);
            }
        }

        return request;
    }

    /**
     * This function is used to create the string using the data passed as parameter
     * @param command
     * @param parameters
     * @return
     */
    private String buildString(String command, String... parameters){
        StringBuilder str = new StringBuilder();
        str.append(command);
        for(String p : parameters){
            str.append(" ");
            str.append(p);
        }
        return str.toString();
    }

    /**
     * This function is used to send data to a server
     * @param command command that we want to send to the server
     * @param ip ip of the server
     * @param port port of the server
     * @param parameters data
     */
    private void sendRequest(String command, String ip, int port, String... parameters){
        String toSend;
        if(parameters.length != 0)
            toSend = buildString(command, parameters);
        else
            toSend = command;
        try {
            write(toSend, ip, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This is used to write data to the socket of a server
     * @param toSend
     * @param ip
     * @param port
     * @throws IOException
     */
    private void write(String toSend, String ip, int port, Object... object) throws IOException {
        Socket toServer = new Socket(ip, port);
        ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(toServer.getOutputStream()));
        oos.writeLong(toSend.length());
        oos.writeUTF(toSend);
        oos.close();
        toServer.close();
    }

    /**
     * This function is called when a new server call the ECS to join the network
     * @param request
     * @param remoteIP
     * @param remoteIn
     * @param out
     */
     public void addNewServer(String[] request, String remoteIP, ObjectInputStream remoteIn, ObjectOutputStream out) {

        if(request.length != 4){
            // TODO Send error message to the server
            sendErrorMessage();
        } else{
            int port = Integer.parseInt(request[2]);
            int intraPort = Integer.parseInt(request[3]);
            logger.info(remoteIP);
            String newHash = computeHash(remoteIP, port);
            logger.info(newHash);

            if(index.contains(newHash)){
                sendMetadata(out, null);
                return;
            }

            // Add the new server to the ECS's index
            if(index.addServer(newHash, remoteIP, intraPort, port)){
                // Send the metadata to the server that wants to join the network
                Metadata metadata = index.generateMetadata();
                sendMetadata(out, metadata);
                // Get the successor's data
                Map.Entry<String, DataMap> successor = index.getSuccessor(newHash);
                String successorKey = successor.getKey();
                DataMap successorValue = successor.getValue();
                // Send the lock request to the successor then wait for the ack
                // from the new server and then release the lock on the successor
                if(!successorKey.equals(newHash)){
                    if(remoteIn != null) {
                        sendRequest("LOCK", successorValue.getIp(), successorValue.getIntraPort(), remoteIP, String.valueOf(intraPort), newHash, successorValue.getStartIndex());
                        if (waitAck(remoteIn)) {
                            sendMetadataToAll(metadata, newHash);
                            sendRequest("RELEASE_LOCK", successorValue.getIp(), successorValue.getIntraPort());
                        }
                    }
                }
                else{
                    // If the server is the first one in the network we just wait for the ack
                    waitAck(remoteIn);
                }
            }
            else{
                // TODO
                sendErrorMessage();
            }
        }
    }




    private void sendErrorMessage(){
        // TODO
    }

    /**
     * This function is called when we have to wait a server that
     * has to send us an ack
     * @param remoteIn
     * @return
     */
    private boolean waitAck(ObjectInputStream remoteIn){
        if(remoteIn!=null) {
            String[] reply = read(remoteIn);
            if(reply[0].equals("ACK")){
                return true;
            }
        }
        return false;
    }

    /**
     * This function is used to send metadata to a SINGLE server
     * @param out
     * @param metadata
     * @return
     */
    public boolean sendMetadata(ObjectOutputStream out, Metadata metadata) {
        if(out!=null) {
            try {
                out.writeObject(metadata);
                out.flush();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /**
     * This function is used to send the data to all the servers that are in the network
     * @param metadata
     * @param hash
     * @return
     */
    public boolean sendMetadataToAll(Metadata metadata, String hash) {

        index.getServers().forEach(p -> {
            if (!p.getFirst().equals(hash)) {
                Socket toServer = null;
                try {
                    toServer = new Socket(p.getSecond().getFirst(), p.getSecond().getSecond());
                    ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(toServer.getOutputStream()));
                    out.flush();
                    out.writeLong("METADATA".length());
                    out.writeUTF("METADATA");
                    sendMetadata(out, metadata);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        return true;
    }

    /**
     * This function is called when we launch the ECS
     */
    private void initECS(){
        try {
            serverSocket = new ServerSocket(ecsPort);
        } catch(BindException e){
            logger.severe("Address already in use");
            System.exit(-1);
        } catch (IllegalArgumentException | IOException e) {
            logger.severe("An error occurred");
            System.exit(-1);

        }
    }

    /**
     * This function is used to compute the hash of the ip, port
     * @param ip
     * @param port
     * @return
     */
    public String computeHash(String ip, int port){
        logger.info("Computing Hash: " + ip + " - " + port);

        MessageDigest md = null;
        try {
            String toHash = ip +
                    port;
            logger.info("Computed Hash " + toHash );

            md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(toHash.getBytes());
            return byteToHex(messageDigest);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * This function is used when a server wants to leave the network
     * @param request
     * @param ip
     * @param ois
     * @param oos
     * @return
     * @throws RemoteException
     */
    @Override
    public String shutdownServer(String[] request, String ip, ObjectInputStream ois, ObjectOutputStream oos) throws RemoteException {
        int port = Integer.parseInt(request[1]);
        int intraPort = Integer.parseInt(request[2]);
        String hash = computeHash(ip, port);
        Map.Entry<String, DataMap> nextNode = index.getSuccessor(hash);
        String hashSuccessor = nextNode.getKey();
        DataMap successor = nextNode.getValue();

        index.removeServer(hash);
        // new metadata
        Metadata metadata = index.generateMetadata();

        Socket socket = null;
        ObjectOutputStream oosSuccessor = null;

        // Metadata update to the successor
        if (ois != null) {
            if (index.size() != 0) {
                sendMetadata(socket, oosSuccessor, metadata, successor);

                // Invoke transfer of data
                sendRequest("LOCK", ip, intraPort);

                // Update all the metadata
                sendMetadataToAll(metadata, hashSuccessor);
            } else {
                sendRequest("FREE", ip, intraPort);
            }
        }

        return null;
    }



    private void sendMetadata(Socket socket, ObjectOutputStream oosSuccessor, Metadata metadata, DataMap successor) {
        try {
            socket = new Socket(successor.getIp(), successor.getIntraPort());
            oosSuccessor = new ObjectOutputStream(socket.getOutputStream());
            oosSuccessor.flush();
            oosSuccessor.writeLong("METADATA".length());
            oosSuccessor.writeUTF("METADATA");
            sendMetadata(oosSuccessor, metadata);
        } catch (IOException e) {
            logger.warning("The server is not available");
            if(socket != null){
                try {
                    socket.close();
                    if(oosSuccessor != null)
                        oosSuccessor.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }


}

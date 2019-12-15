package de.tum.i13.server.kv;

import de.tum.i13.server.Cache.Cache;
import de.tum.i13.server.FileStorage.FileStorage;
import de.tum.i13.shared.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.tum.i13.shared.Utility.computeHash;
import static de.tum.i13.shared.Utility.getFreePort;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class KVIntraCommunication implements Runnable {
    private Logger logger = null;
    private ServerStatus serverStatus;
    private String myAddress;
    private InetSocketAddress ecs;
    private int myPort;
    private int intraPort;
    private int myPingPort;
    // This socket is used to receive the data from another server and from the ECS
    private ServerSocket serverSocket;
    private Metadata metadata;
    private FileStorage fileStorage;
    private ArrayList<Pair<String, String>> data;
    private ArrayList<Pair<String, String>> toBeRemoved = new ArrayList<>();
    private ObjectInputStream ECSois = null;
    private ObjectOutputStream ECSoos = null;
    private Socket ECSSocket = null;
    private boolean isInterrupted = false;
    private boolean shutdown = false;
    private String myHash;
    private Cache cache;
    private boolean endPing = true;
    private Thread pingReply;
    ServerSocket pingSocket = null;

    KVIntraCommunication(Config cfg, ServerStatus serverStatus, Metadata metadata, FileStorage fileStorage, Logger logger, Cache cache) {
        this.serverStatus = serverStatus;
        this.myAddress = cfg.listenaddr;
        this.fileStorage = fileStorage;
        this.ecs = cfg.bootstrap;
        this.myPort = cfg.port;
        this.metadata = metadata;
        this.logger = logger;
        this.cache = cache;
    }


    @Override
    public void run() {
        logger.info("Starting KVINTRACOMMUNICATION");
        init();
        initPingPort();
        connectToECS();
        if (!serverStatus.checkEqual(Constants.NO_CONNECTION)) {
            joinNetwork();
            readMetadata(ECSois);
            processRequest();
        }
    }

    /**
     * This function is useful because we need to start a thread to reply to the pinger
     */
    private void initPingPort() {
        pingReply = new Thread(() -> {

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    pingSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                endPing = false;
            }));

            try {
                pingSocket = new ServerSocket();
                if (this.myAddress != null) {
                    SocketAddress address = new InetSocketAddress(this.myAddress, this.myPingPort);
                    pingSocket.bind(address);
                }
                while (endPing) {
                    try {
                        Socket s = pingSocket.accept();
                        s.close();
                    } catch (Exception e) {
                        endPing = false;
                    }
                }
            } catch (IOException e) {
                endPing = false;
            }

        });
        pingReply.start();
    }

    /**
     * This function is used to process all the possible requests that the server
     * can receive from another server
     */
    private void processRequest() {
        while (!isInterrupted) {
            try {
                logger.info("Process Request " + this.myHash);

                Socket s = serverSocket.accept();
                logger.info("new connection accepted " + s.getInetAddress());
                ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
                oos.flush();
                ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(s.getInputStream()));
                String[] command = read(ois);
                if (command != null && command.length != 0) {
                    logger.info("New Command: " + command[0]);
                    logger.info("New request received " + command[0]);
                    switch (command[0]) {
                        case ("LOCK"):
                            sendData(command);
                            break;
                        case ("RECEIVE_DATA"):
                            receiveKVPairs(ois);
                            break;
                        case ("RECEIVE_DATA_REPLICA"):
                            receiveReplica(ois, oos, command);
                            break;
                        case ("RECEIVE_SINGLE_REPLICA"):
                            receiveSingleReplica(ois, oos);
                            break;
                        case ("RELEASE_LOCK"):
                            releaseLock();
                            break;
                        case ("METADATA"):
                            readMetadata(ois);
                            break;
                        case ("FREE"):
                            System.exit(0);
                            break;
                        case ("DELETE_REPLICAS"):
                            deleteReplicas(ois);
                            break;
                        case ("RECEIVE_DATA_SHUTDOWN"):
                            receiveDataShutdown(ois, oos);
                            break;
                        case ("RECEIVE_REPLICA_SHUTDOWN"):
                            replicaShutdown(ois, oos);
                            break;
                        case ("METADATA_PING"):
                            metadataPing(ois, oos);
                            break;
                        default:
                            break;
                    }

                }

                if (shutdown) {
                    closeAll();
                }
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }
    }

    /**
     * This function is used to send the data to the new server that is joining
     * the network.
     * @param command the String[] with the full request received by the ECS
     */
    private void sendData(String[] command) {
        logger.info("sending file data");
        if (command.length == 5) {
            String ip = command[1];
            int port = Integer.parseInt(command[2]);
            String newServerEndIndex = command[3];
            String newServerStartIndex = command[4];
            serverStatus.setStatus(Constants.LOCKED);
            // When we add the third server in the network we have to send our data to the new server
            // and also to our successor
            if (metadata.size() == 2) {
                reallocateData(ip, port, newServerEndIndex, this.myHash, "NEW");
            }
            // In this case we have more than 2 servers in our network so we only have to send our data
            // and the replicas that I own in to the new server.
            // I don't have to send data to my successor but I have to send to my successor a message
            // because it has to delete some replicas
            else if (metadata.size() > 2) {
                reallocateData(ip, port, newServerEndIndex, newServerStartIndex, "NEW_AND_REPLICA");
            } else {
                reallocateData(ip, port, newServerEndIndex, newServerStartIndex, "NEW");
            }
        } else {
            handleError("Invalid Number of Parameters", Constants.INACTIVE);
        }
    }

    /**
     * This method is called when we want to receive a KVPair
     * @param ois ObjectInputStream from where we receive data
     */
    private void receiveKVPairs(ObjectInputStream ois) {
        logger.info("Receiving key,value pairs");
        try {
            ArrayList<Pair<String, String>> data = (ArrayList<Pair<String, String>>) ois.readObject();
            logger.info("DATA SIZE " + data.size());
            for (Pair<String, String> p : data) {
                fileStorage.put(p.getFirst(), p.getSecond());
            }
            sendAck();
        } catch (IOException | ClassNotFoundException e) {
            handleError("An error occurred while receiving the data", Constants.INACTIVE);
        }
    }

    /**
     * This method is called to receive a replica of a kvpair
     * @param ois ObjectInputStream from where we receive data
     * @param oos ObjectInputStream where we can send data
     * @param commands the String[] with the full request received by the ECS
     */
    private void receiveReplica(ObjectInputStream ois, ObjectOutputStream oos, String[] commands) {
        logger.info("Receiving replicas: key,value pairs");
        try {

            ArrayList<Pair<String, String>> data = (ArrayList<Pair<String, String>>) ois.readObject();
            logger.info("DATA SIZE " + data.size());
            for (Pair<String, String> p : data) {
                if(fileStorage.put(p.getFirst(), p.getSecond())>=0){
                    cache.put(p.getFirst(), p.getSecond());
                }
            }
            int numReplica = 0;
            if (commands.length == 2) {
                numReplica = Integer.parseInt(commands[1]);
                if (numReplica > 0) {
                    numReplica--;
                    Map.Entry<String, DataMap> successor = metadata.getMySuccessor(this.myHash);
                    Socket s = new Socket(successor.getValue().getIp(), successor.getValue().getIntraPort());
                    ObjectOutputStream oosSucc = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
                    oos.flush();
                    sendKVPairs(Constants.HEX_END_INDEX, Constants.HEX_START_INDEX, oosSucc, "RECEIVE_DATA_REPLICA " + numReplica);
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            handleError("An error occurred while receiving the data", Constants.INACTIVE);
        }
    }

    /**
     * This method is called when we want to receive a single KVPair from another server
     * @param ois ObjectInputStream of the server that is sending data
     * @param oos ObjectOutputStream of the server that is sending data
     */
    private void receiveSingleReplica(ObjectInputStream ois, ObjectOutputStream oos) {
        logger.info("Receiving replicas: key,value pairs");
        try {
            Pair<Integer, Pair<String, String>> data = (Pair<Integer, Pair<String, String>>) ois.readObject();
            int numReplicas = data.getFirst() - 1;
            Pair<String, String> kvPair = data.getSecond();
            if(fileStorage.put(kvPair.getFirst(), kvPair.getSecond())>=0){
                cache.put(kvPair.getFirst(), kvPair.getSecond());
            }
            // Forward pair to my successor
            if (numReplicas > 0) {
                Common.sendReplica(kvPair.getFirst(), kvPair.getSecond(), this.myHash, numReplicas, metadata);
            }
        } catch (IOException | ClassNotFoundException e) {
            handleError("An error occurred while receiving the data", Constants.INACTIVE);
        }
    }

    /**
     * This method is called when we want to release the lock from the server
     */
    private void releaseLock() {
        logger.info("Releasing the lock");
        if (metadata.size() < 3)
            data.forEach((p) -> fileStorage.remove(p.getFirst()));

        serverStatus.setStatus(Constants.ACTIVE);
    }

    /**
     * This function is called when I have to read a new set of metadata from the
     * ECS or from another server. If I'm the first server in the network I don't
     * have to send any message to the others servers and so I immediately send the
     * ack to the ECS.
     */
    private void readMetadata(ObjectInputStream ois) {
        logger.info("Reading Metadata");

        try {
            Metadata mtd = (Metadata) ois.readObject();
            logger.info("Metadata Read");

            this.metadata.addAll(mtd);
            if (metadata == null) {
                handleError("An error occurred: you are connected with the same server", Constants.INACTIVE);
            } else if (metadata.size() == 1) {
                sendAck();
            }
            if (metadata.size() == 3 && this.serverStatus.checkEqual(Constants.LOCKED)) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(this::firstReplica);
            }
            logger.info(metadata.toString());
        } catch (IOException | ClassNotFoundException e) {
            handleError("An error occurred while updating metadata", Constants.NO_CONNECTION);
        }
    }

    /**
     * This method is called to delete replicas from a server
     * @param ois ObjectInputStream from where we want to read
     */
    private void deleteReplicas(ObjectInputStream ois) {
        try {
            ArrayList<Pair<String, String>> replicas = (ArrayList<Pair<String, String>>) ois.readObject();
            for (Pair<String, String> p : replicas) {
                fileStorage.checkAndDelete(p.getFirst());
            }
        } catch (IOException | ClassNotFoundException e) {
            //e.printStackTrace();
        }
    }


    /**
     * This function is called whem we receive some data from one of our predecessor.
     * @param ois ObjectInputStream of the server that is sending data to us
     * @param oos ObjectOutputStream of the server that is sending data to us
     */
    private void receiveDataShutdown(ObjectInputStream ois, ObjectOutputStream oos) {
        logger.info("Receiving data shutdown");

        try {
            ArrayList<Pair<Integer, Pair<String, String>>> toSend = new ArrayList<>();
            ArrayList<Pair<String, String>> data = (ArrayList<Pair<String, String>>) ois.readObject();
            logger.info("DATA SIZE " + data.size());
            Map.Entry<String, DataMap> successor = metadata.getMySuccessor(this.myHash);
            for (Pair<String, String> p : data) {
                if (successor != null && metadata.size() >= 3) {
                    // I'm the new responsible for this data. Send the data to my successor
                    // then my successor will send the data to its successor
                    if (metadata.isResponsible(this.myHash, p.getFirst())) {
                        toSend.add(new Pair<>(Constants.NUM_REPLICAS, p));
                    }
                    // My successor is a replica for this data. It is the second
                    // replica because I'm not responsible for the file.
                    else if (metadata.isReplica(successor.getValue().getEndIndex(), p.getFirst())) {
                        toSend.add(new Pair<>(Constants.NUM_REPLICAS - 1, p));
                    }
                }

                fileStorage.put(p.getFirst(), p.getSecond());
            }

            sendAckTo(oos);

            if (successor != null && metadata.size() >= 3) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        Socket s = new Socket(successor.getValue().getIp(), successor.getValue().getIntraPort());
                        ObjectOutputStream oSucc = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
                        Common.sendKVPairs(toSend, oSucc, "RECEIVE_REPLICA_SHUTDOWN");
                        oSucc.close();
                    } catch (IOException e) {
                        // e.printStackTrace();
                    }
                });
            }

        } catch (IOException | ClassNotFoundException e) {
            handleError("An error occurred while receiving the data", Constants.INACTIVE);
        }
    }

    /**
     * This function is called when the ECS is shutting down a server and
     * we want move the data to the successors.
     * @param ois ECS's ObjectInputStream
     * @param oos ECS's ObjectOutputStream
     */
    private void replicaShutdown(ObjectInputStream ois, ObjectOutputStream oos) {
        ArrayList<Pair<Integer, Pair<String, String>>> data;
        try {
            ArrayList<Pair<Integer, Pair<String, String>>> toSend = new ArrayList<>();
            data = (ArrayList<Pair<Integer, Pair<String, String>>>) ois.readObject();
            data.forEach(p -> {
                fileStorage.put(p.getSecond().getFirst(), p.getSecond().getSecond());
                cache.put(p.getSecond().getFirst(), p.getSecond().getSecond());
                if (p.getFirst() == Constants.NUM_REPLICAS) {
                    toSend.add(new Pair<>(Constants.NUM_REPLICAS - 1, p.getSecond()));
                }
            });
            Map.Entry<String, DataMap> successor = metadata.getMySuccessor(this.myHash);
            if (successor != null && toSend.size() != 0) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        Socket s = new Socket(successor.getValue().getIp(), successor.getValue().getIntraPort());
                        ObjectOutputStream oSucc = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
                        Common.sendKVPairs(toSend, oSucc, "RECEIVE_REPLICA_SHUTDOWN");
                        oSucc.close();
                    } catch (IOException e) {
                        //e.printStackTrace();
                    }
                });
            }
        } catch (IOException | ClassNotFoundException e) {
            //e.printStackTrace();
        }
    }

    /**
     * This function is called when a server is crashed and we need to restore the data in
     * the network. This server is now the responsible for the data of the crashed server
     * and it has to send all the data to its successor.
     * @param ois ObjectInputStream of the ECS
     * @param oos ObjectOutputStream of the ECS
     */
    private void metadataPing(ObjectInputStream ois, ObjectOutputStream oos) {
        Metadata preMetadata = metadata;

        try {

            Metadata mtd = (Metadata) ois.readObject();
            this.metadata.addAll(mtd);

            if (!metadata.getMyPredecessor(this.myHash).equals(preMetadata.getMyPredecessor(this.myHash))) {
                ArrayList<Pair<String, String>> myData = fileStorage.getAll();
                ArrayList<Pair<Integer, Pair<String, String>>> toSend = new ArrayList<>();
                Map.Entry<String, DataMap> predecessor = metadata.getMyPredecessorEntry(this.myHash);

                if (myData.size() > 0) {
                    myData.forEach(p -> {
                        if (metadata.isResponsible(predecessor.getKey(), computeHash(p.getFirst()))) {
                            toSend.add(new Pair<>(1, p));
                        } else {
                            toSend.add(new Pair<>(2, p));

                        }

                    });

                    try {
                        Map.Entry<String, DataMap> successor = metadata.getMySuccessor(this.myHash);
                        Socket s = new Socket(successor.getValue().getIp(), successor.getValue().getIntraPort());
                        ObjectOutputStream oSucc = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
                        Common.sendKVPairs(toSend, oSucc, "RECEIVE_REPLICA_SHUTDOWN");
                        oSucc.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    /*******************************************************************************************************/



    /**
     * This functions is called when we want to send an ACK to a specific server
     * @param oos ObjectOutputStream of the server that we want to contact
     */
    private void sendAckTo(ObjectOutputStream oos) {
        try {
            logger.info("send Ack");
            String toSend = "ACK";
            long len = toSend.length();
            if (oos != null) {
                Common.write(len, toSend, oos);
            }

            logger.info("Trainsfer completed!".toUpperCase());
        } catch (Exception e) {
            sendAck();
        }
    }

    /**
     * This function is called when we want to send data to a specific server
     */
    private void shutdownTransfer() {
        logger.info("sending file data to my successor");
        serverStatus.setStatus(Constants.LOCKED);
        Map.Entry<String, DataMap> successor = metadata.getMySuccessor(this.myHash);
        ObjectInputStream ois = null;
        try {
            Socket s = new Socket(successor.getValue().getIp(), successor.getValue().getIntraPort());
            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
            oos.flush();
            ois = new ObjectInputStream(new BufferedInputStream(s.getInputStream()));
            sendKVPairs(Constants.HEX_END_INDEX, Constants.HEX_START_INDEX, oos, "RECEIVE_DATA_SHUTDOWN");
            serverStatus.setStatus(String.valueOf(Constants.SHUTDOWN));
            fileStorage.clear();
            /*String ack = readShutdown(ois);
            if (ack != null) {
                if (ack.equals("ACK")) {
                    sendAck();
                    serverStatus.setStatus(String.valueOf(Constants.SHUTDOWN));
                    fileStorage.clear();
                }
            }*/
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }


    /**
     * This method is called when we add the third server in the network and we want to start the replication
     */
    private void firstReplica() {

        Map.Entry<String, DataMap> successor = metadata.getMySuccessor(this.myHash);
        try {
            Socket s = new Socket(successor.getValue().getIp(), successor.getValue().getIntraPort());
            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
            oos.flush();
            sendKVPairs(Constants.HEX_END_INDEX, Constants.HEX_START_INDEX, oos, "RECEIVE_DATA_REPLICA " + Constants.NUM_REPLICAS);
        } catch (IOException e) {
            //e.printStackTrace();
        }

    }


    /**
     * This function is called when we need to move data between the servers
     * @param ip ip of the server
     * @param port port of the server
     * @param newServerEndIndex end index of the server that is joining the network
     * @param newServerStartIndex start index of the server that is joining the network
     * @param type operation's type
     */
    private void reallocateData(String ip, int port, String newServerEndIndex, String newServerStartIndex, String type) {
        try {
            Socket s = new Socket(ip, port);
            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
            oos.flush();
            logger.info("SEND DATA HASH: " + newServerEndIndex);
            if (newServerEndIndex != null) {

                // Send data to new server
                if (type.equals("NEW")) {

                    sendKVPairs(newServerEndIndex, newServerStartIndex, oos, "RECEIVE_DATA");
                } else if (type.equals("NEW_AND_REPLICA")) {
                    sendKVPairs(newServerEndIndex, newServerStartIndex, oos, "RECEIVE_DATA_NEW_AND_REPLICA");
                } else {
                    sendKVPairs(newServerEndIndex, newServerStartIndex, oos, "RECEIVE_DATA_REPLICA");
                }
                oos.close();
                s.close();
            }
            logger.info("Data sent");
        } catch (IOException | ArrayIndexOutOfBoundsException e) {
            //e.printStackTrace();
            logger.severe("Error");
        }
    }


    /**
     * This functions is the first function that is called when the thread is
     * started. It is used to create the server Socket that we need to communicate
     * with the others servers.
     *
     * @throws IOException
     */
    private void init() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));

        logger.info("Init socket");
        try {

            intraPort = getFreePort();
            myPingPort = getFreePort();
            this.serverSocket = new ServerSocket();
            if (myAddress != null) {
                SocketAddress address = new InetSocketAddress(myAddress, intraPort);
                serverSocket.bind(address);
            }
            logger.info("Init done");
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }


    /**
     * This method is called when we want to close the sockets
     * and the Input/OutputStream to the ECS
     */
    private void closeAll() {
        try {
            if (ECSSocket != null) {
                if (ECSSocket.isConnected()) {
                    ECSSocket.close();
                    ECSSocket = null;
                }
            }
            if (ECSois != null)
                ECSois.close();
            if (ECSoos != null) {
                ECSoos.close();
                ECSoos = null;
            }

        } catch (IOException e) {
            logger.severe("error");
        }
    }


    /**
     * This method is called when we want to shutdown the server
     */
    public void close() {

        try {

            if (serverStatus.checkEqual(Constants.INACTIVE)) {
                isInterrupted = true;
            } else if (serverStatus.checkEqual(Constants.NO_CONNECTION)) {
                logger.info("An error occurred while connecting to the ECS");
            } else {
                shutdown = true;
                logger.info("Sending shutdown to ECS " + this.myHash);
                sendShutdown();

            }
            if (metadata.size() == 1) {
                isInterrupted = true;
                serverStatus.setStatus(String.valueOf(Constants.SHUTDOWN));
            } else {
                sendDataToSuccessor();
            }
            endPing = false;
            try {
                pingSocket.close();
                pingReply.interrupt();
            } catch (Exception ignored) {

            }
            closeAll();
            logger.info("Closing completed");
        } catch (Exception e) {
            //e.printStackTrace();
        }

    }

    /**
     * This method is called when we want to send data to the successor
     * of a node
     * @throws IOException
     */
    private void sendDataToSuccessor() throws IOException {
        if (readShutdown(ECSois).equals("SHUTDOWN_TRANSFER")) {
            shutdownTransfer();
        }

    }

    /**
     * This method is called when we have to receive some data when
     * a server is leaving from the network.
     * @param ois ObjectInputStream from where we want to read
     * @return String[] with all the informations that we have read
     * @throws IOException
     */
    private String readShutdown(ObjectInputStream ois) throws IOException {
        long toRead = ois.readLong();
        String command = null;
        String[] splitted = null;
        if (ois.available() > 0) {
            do {
                command = ois.readUTF();
            } while (command.length() < toRead);
        }

        if (command != null) {
            splitted = command.split(" ");
        }
        return splitted[0];
    }


    /**
     * This method is called when the server wants to leave the network
     */
    private void sendShutdown() {
        closeAll();
        connectToECS();
        if (ECSSocket != null) {
            logger.info("Sending shutdown " + this.myHash);
            if (!serverStatus.checkEqual(Constants.NO_CONNECTION)) {
                try {
                    String toSend = "shutdownServer " + this.myPort + " " + this.intraPort;
                    long len = toSend.length();
                    Common.write(len, toSend, ECSoos);
                } catch (IOException e) {
                    //e.printStackTrace();
                }
            }
        }


    }

    /**
     * This function is used to open the connection toward the server
     */
    private void connectToECS() {
        logger.info("Connecting to ecs");
        Socket s = openSocket(this.ecs.getHostName(), this.ecs.getPort());
        try {
            ECSSocket = s;
            if (ECSSocket.isConnected()) {
                ECSoos = new ObjectOutputStream(new BufferedOutputStream(ECSSocket.getOutputStream()));
                ECSoos.flush();
                ECSois = new ObjectInputStream(new BufferedInputStream(ECSSocket.getInputStream()));
            } else {
                throw new IOException();
            }
        } catch (IOException e) {
            //e.printStackTrace();
            handleError("An error occurred while connecting to the ECS.....", Constants.NO_CONNECTION);
        }

    }

    /**
     * This method is called to open a Socket
     * @param hostName ip of the server
     * @param port port of the server
     * @return the opened socket
     */
    private Socket openSocket(String hostName, int port) {
        try {
            return new Socket(hostName, port);
        } catch (IOException e) {
            //e.printStackTrace();
            handleError("An error occurred while connecting to the ADDRESS", Constants.NO_CONNECTION);
            return null;
        }
    }

    /**
     * This function is called when I have to handle an error
     */
    private void handleError(String errorMessage, String status) {
        logger.log(Level.SEVERE, errorMessage);
        serverStatus.setStatus(status);
        if (!shutdown) {
            System.exit(-1);
        }
    }

    /**
     * The server calls joinNetwork to join the network.
     */
    private void joinNetwork() {
        logger.info("Joining network");
        try {
            String toSend = "newServer " + this.myAddress + " " + this.myPort + " " + this.intraPort + " " + this.myPingPort;
            this.myHash = Utility.computeHash(this.myAddress, this.myPort);
            long len = toSend.length();
            Common.write(len, toSend, ECSoos);

        } catch (IOException e) {
            //e.printStackTrace();
        }
    }


    /**
     * This function is used to send the ACK to the ECS.
     *
     * @throws IOException
     */
    private void sendAck() {
        try {
            logger.info("send Ack");
            String toSend = "ACK";
            long len = toSend.length();
            if (ECSoos != null) {
                Common.write(len, toSend, ECSoos);
            }
            closeAll();
            serverStatus.setStatus(Constants.ACTIVE);
            logger.info("Joining process completed!".toUpperCase());
        } catch (Exception e) {
            handleError("An error occurred while sending the ACK", Constants.INACTIVE);
        }
    }


    /**
     * This function is called to read from an ObjectInputStream
     * @param remoteIn the ObjectInputStream from where we want to read
     * @return the Sting[] that contains the data that we have read
     */
    private String[] read(ObjectInputStream remoteIn) {
        logger.info("Reading data from ObjectInputStream " + this.myHash);
        long toRead = 0;
        String command = null;
        String[] splitted = null;
        try {
            if (!shutdown)
                toRead = remoteIn.readLong();

            if (!shutdown && toRead > 0 && remoteIn.available() > 0) {
                do {
                    command = remoteIn.readUTF();
                } while (command.length() < toRead);
            }

            if (command != null) {
                splitted = command.split(" ");
            }
        } catch (IOException e) {

        }
        return splitted;
    }


    /**
     * This method is called when we want to send a KVPair to a specific server
     * @param newServerEndIndex End index of the new server
     * @param newServerStartIndex Start index of the new server
     * @param oos ObjectOutputStream of the new server
     * @param type operation's type
     * @throws IOException
     */
    private void sendKVPairs(String newServerEndIndex, String newServerStartIndex, ObjectOutputStream oos, String type) throws IOException {
        data = new ArrayList<>();

        data = fileStorage.getRange(newServerEndIndex, newServerStartIndex);
        if (type.equals("RECEIVE_DATA_NEW_AND_REPLICA")) {
            // I get my predecessor. I need this data because I have the replicas of my predecessor.
            Pair<String, String> predecessorHash = metadata.getMyPredecessor(this.myHash);
            // I get the predecessor of my predecessor. I need this because I can also have the replicas
            // of precedecessor(my predecessor)
            Pair<String, String> predPredHash = metadata.getMyPredecessor(predecessorHash.getSecond());
            Pair<ArrayList<Pair<String, String>>, ArrayList<Pair<String, String>>> replicas = fileStorage.getReplicas(predecessorHash, predPredHash);

            data.addAll(replicas.getFirst());
            data.addAll(replicas.getSecond());
            toBeRemoved = replicas.getSecond();
            type = "RECEIVE_DATA";
        }
        oos.flush();
        oos.writeLong(type.length());
        oos.writeUTF(type);
        oos.writeObject(data);
        oos.flush();
    }



    /**
     * TEST METHOD. This is used only in a single test
     */
    void simulateSIGKILL() {
        endPing = false;
        try {
            pingSocket.close();
        } catch (IOException e) {
            endPing = false;
        }
    }
}

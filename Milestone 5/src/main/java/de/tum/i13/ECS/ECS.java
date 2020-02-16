package de.tum.i13.ECS;

import de.tum.i13.shared.DataMap;
import de.tum.i13.shared.Metadata;
import de.tum.i13.shared.Utility;

import java.io.*;
import java.math.BigInteger;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import static de.tum.i13.shared.LogSetup.setupLogging;

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
    private int ecsPort;
    private boolean shutdown = false;
    private boolean ready = false;
    private FileHandler fileHandler;

    /**
     * ECS constructor
     *
     * @param port    port of the ECS
     * @param logging Logger of the ECS
     */
    public ECS(int port, String logging) {
        this.ecsPort = port;
        Path p = new File("ecs.log").toPath();
        fileHandler = setupLogging(p, logging, logger);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("SHUTDOWN HOOK");
            shutdown = true;
            try {
                fileHandler.close();
                if (serverSocket != null)
                    serverSocket.close();
                closeAll(in, out, socket);
            } catch (IOException e) {

            }
            logger.info("Closing log file");
            fileHandler.close();

        }));
    }

    /**
     * Test function used in the tests to get the metadata from the ECS
     *
     * @return metadata from the ECS
     */
    public Metadata getMetadata() {
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
     *
     * @return true or false
     */
    public boolean isReady() {
        return this.ready;
    }

    /**
     * This function is used to handle all the requests by the ECS
     */
    private void handleRequests() {
        while (true) {
            try {
                if (!shutdown) {
                    logger.info("Accepting new connections....");
                    String remoteIP = acceptConnection();
                    String[] request = read(in);
                    if (request != null) {
                        logger.info("Request received: " + request[0]);
                        switch (request[0]) {
                            case ("newServer"):
                                addNewServer(request, remoteIP, in, out);
                                break;
                            case ("shutdownServer"):
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
                if (!shutdown) {
                    System.exit(-1);
                }
            } catch (RemoteException e) {
                logger.severe("Error during communication");
                if (!shutdown) {
                    System.exit(-1);
                }
            } catch (IOException | NullPointerException e) {
                logger.severe("Error during connection");
                if (!shutdown) {
                    System.exit(-1);
                }
            }
        }
    }

    /**
     * This is used to close all the streams and the socket
     *
     * @param in     ObjectInputStream that we want to close
     * @param out    ObjectOutputStream that we want to close
     * @param socket socket that we want to close
     */
    private void closeAll(ObjectInputStream in, ObjectOutputStream out, Socket socket) throws IOException {
        if (in != null) {
            in.close();
            in = null;
        }
        if (out != null) {
            out.close();
            out = null;
        }
        if (socket != null) {
            if (socket.isConnected())
                socket.close();
        }
    }

    /**
     * This function accepts the connections from the servers
     *
     * @return the Ip of the server that is connected with the ECS
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
    private void startPinger() {
        pinger = new Thread(new PingThread(index));
        pinger.start();
    }

    /**
     * This function reads all the data from the ObjectInputStream passed as parameter
     *
     * @param remoteIn The ObjectInputStream from which we want to read
     * @return Data that we have read
     */
    private String[] read(ObjectInputStream remoteIn) {
        Long toRead;
        String command;
        String[] request = null;
        try {
            if (!shutdown && remoteIn != null) {
                toRead = remoteIn.readLong();
                do {
                    command = remoteIn.readUTF();
                } while (command.length() < toRead);
                request = command.split(" ");
            }

        } catch (IOException e) {
            if (!shutdown) {
                System.exit(-1);
            }
        }
        return request;
    }

    /**
     * This function is called when a new server call the ECS to join the network
     *
     * @param request  the String[] with the full request received by the ECS
     * @param remoteIP IP of the server that wants to join the network
     * @param remoteIn ObjectInputStream of the server that wants to join the network
     * @param out      ObjectOutputStream of the server that wants to join the network
     */
    public void addNewServer(String[] request, String remoteIP, ObjectInputStream remoteIn, ObjectOutputStream out) {

        if (request.length != 5) {
            sendErrorMessage();
        } else {
            int port = Integer.parseInt(request[2]);
            int intraPort = Integer.parseInt(request[3]);
            int pingPort = Integer.parseInt(request[4]);
            logger.info(remoteIP);
            String newHash = Utility.computeHash(remoteIP, port);
            logger.info(newHash);

            if (index.contains(newHash)) {
                sendMetadata(out, null);
                return;
            }

            // Add the new server to the ECS's index
            if (index.addServer(newHash, remoteIP, intraPort, port, pingPort)) {
                // Send the metadata to the server that wants to join the network
                Metadata metadata = index.generateMetadata();
                sendMetadata(out, metadata);
                // Get the successor's data
                Map.Entry<String, DataMap> successor = index.getSuccessor(newHash);
                String successorKey = successor.getKey();
                DataMap successorValue = successor.getValue();
                // Send the lock request to the successor then wait for the ack
                // from the new server and then release the lock on the successor
                BigInteger succStart = new BigInteger(successorValue.getEndIndex(), 16);
                succStart = succStart.add(BigInteger.ONE);
                String successorStart = succStart.toString(16);

                if (!successorKey.equals(newHash)) {
                    if (remoteIn != null) {
                        sendRequest("LOCK", successorValue.getIp(), successorValue.getIntraPort(), remoteIP, String.valueOf(intraPort), newHash, successorStart);
                        if (waitAck(remoteIn)) {
                            logger.info("Ack received");
                            sendMetadataToAll(metadata, newHash);
                            sendRequest("RELEASE_LOCK", successorValue.getIp(), successorValue.getIntraPort());
                        }

                    }
                } else {
                    // If the server is the first one in the network we just wait for the ack
                    waitAck(remoteIn);
                }
            } else {
                // TODO
                sendErrorMessage();
            }
        }
    }


    private void sendErrorMessage() {
        // TODO
    }

    /**
     * This function is called when we have to wait a server that
     * has to send us an ack
     *
     * @param remoteIn ObjectInputStream of the server
     * @return true is we received the ack, false otherwise
     */
    private boolean waitAck(ObjectInputStream remoteIn) {
        if (remoteIn != null) {
            String[] reply = read(remoteIn);
            return reply[0].equals("ACK");
        }
        return false;
    }

    /**
     * This function is used to send metadata to a SINGLE server
     *
     * @param out      ObjectOutputStream o the server
     * @param metadata metadata that we want to send
     * @return true if we send metadata false otherwise
     */
    public boolean sendMetadata(ObjectOutputStream out, Metadata metadata) {
        if (out != null) {
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
     *
     * @param metadata metadata that we want to send
     * @param hash     hash of the server that don't need the new metadata
     * @return true if we send metadata false otherwise
     */
    public boolean sendMetadataToAll(Metadata metadata, String hash) {
        logger.info("Sending Metadata to All");
        index.getServers().forEach(p -> {
            if (!p.getFirst().equals(hash)) {
                Socket toServer = null;
                try {
                    toServer = new Socket(p.getSecond().getFirst(), p.getSecond().getSecond());
                    ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(toServer.getOutputStream()));
                    out.flush();
                    out.writeLong("METADATA".length());
                    out.flush();
                    out.writeUTF("METADATA");
                    out.flush();
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
    private void initECS() {
        try {
            serverSocket = new ServerSocket(ecsPort);
        } catch (BindException e) {
            logger.severe("Address already in use");
            System.exit(-1);
        } catch (IllegalArgumentException | IOException e) {
            logger.severe("An error occurred");
            System.exit(-1);

        }
    }


    /**
     * This function is used when a server wants to leave the network
     *
     * @param request the String[] with the full request received by the ECS
     * @param ip      ip of the server that wants to leave the network
     * @param ois     ObjectInputStream of the server that wants to leave the network
     * @param oos     ObjectOutputStream of the server that wants to leave the network
     * @return
     * @throws RemoteException
     */
    @Override
    public String shutdownServer(String[] request, String ip, ObjectInputStream ois, ObjectOutputStream oos) throws RemoteException {
        int port = Integer.parseInt(request[1]);
        int intraPort = Integer.parseInt(request[2]);
        String hash = Utility.computeHash(ip, port);
        index.removeServer(hash);
        Map.Entry<String, DataMap> nextNode = index.getSuccessor(hash);

        if (nextNode != null) {
            String hashSuccessor = nextNode.getKey();
            DataMap successor = nextNode.getValue();

            // new metadata
            Metadata metadata = index.generateMetadata();

            Socket socket = null;
            ObjectOutputStream oosSuccessor = null;

            // Metadata update to the successor
            if (ois != null) {
                if (index.size() != 0) {
                    sendMetadata(socket, oosSuccessor, metadata, successor);

                    // Invoke transfer of data
                    sendRequest("SHUTDOWN_TRANSFER", ip, intraPort);
                    String toSend = "SHUTDOWN_TRANSFER";
                    try {
                        oos.writeLong(toSend.length());
                        oos.writeUTF(toSend);
                        oos.flush();

                        // Update all the metadata
                        sendMetadataToAll(metadata, hashSuccessor);
                    } catch (IOException e) {
                        //e.printStackTrace();
                    }

                } else {
                    sendRequest("FREE", ip, intraPort);
                }
            }
        }


        return null;
    }

    /**
     * This function is used to send the metadata when we receive a shutdown from a server
     *
     * @param socket       Socket that we use to connect to the successor of the node that is leaving the network
     * @param oosSuccessor ObjectOutputStream of the successor
     * @param metadata     metadata that we want to send
     * @param successor    successor of the node that is leaving the network
     */
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
            if (socket != null) {
                try {
                    socket.close();
                    if (oosSuccessor != null)
                        oosSuccessor.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }


    /**
     * This function is used to send data to a server
     *
     * @param command    command that we want to send to the server
     * @param ip         ip of the server
     * @param port       port of the server
     * @param parameters data
     */
    private void sendRequest(String command, String ip, int port, String... parameters) {
        logger.info("Sending request: " + command);
        String toSend;
        if (parameters.length != 0)
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
     * This function is used to create the string using the data passed as parameter
     *
     * @param command    the String[] with the full request received by the ECS
     * @param parameters Optional parameter that we could send to a server
     * @return String that we have to send
     */
    private String buildString(String command, String... parameters) {
        StringBuilder str = new StringBuilder();
        str.append(command);
        for (String p : parameters) {
            str.append(" ");
            str.append(p);
        }
        return str.toString();
    }


    /**
     * This is used to write data to the socket of a server
     *
     * @param toSend String that we want to send
     * @param ip     ip of the server who will receive the message
     * @param port   port of the server who will receive the message
     * @throws IOException
     */
    private void write(String toSend, String ip, int port) throws IOException {
        Socket toServer = new Socket(ip, port);
        ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(toServer.getOutputStream()));
        oos.flush();
        ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(toServer.getInputStream()));

        oos.writeLong(toSend.length());
        oos.writeUTF(toSend);

        oos.flush();

    }


}

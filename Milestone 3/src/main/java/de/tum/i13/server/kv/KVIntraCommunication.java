package de.tum.i13.server.kv;

import de.tum.i13.server.FileStorage.FileStorage;
import de.tum.i13.shared.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.tum.i13.shared.LogSetup.setupLogging;
import static de.tum.i13.shared.Utility.getFreePort;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class KVIntraCommunication implements Runnable {
    public static Logger logger = Logger.getLogger(KVIntraCommunication.class.getName());
    private ServerStatus serverStatus;
    private String myAddress;
    private InetSocketAddress ecs;
    private int myPort;
    private int intraPort;
    // This socket is used to receive the data from another server and from the ECS
    private ServerSocket serverSocket;
    private Metadata metadata;
    private FileStorage fileStorage;
    private ArrayList<Pair<String, String>> data;
    private ObjectInputStream ECSois = null;
    private ObjectOutputStream ECSoos = null;
    private Socket ECSSocket = null;
    private boolean isInterrupted = false;
    private boolean shutdown = false;

    KVIntraCommunication(Config cfg, ServerStatus serverStatus, Metadata metadata, FileStorage fileStorage) {
        this.serverStatus = serverStatus;
        this.myAddress = cfg.listenaddr;
        this.fileStorage = fileStorage;
        this.ecs = cfg.bootstrap;
        this.myPort = cfg.port;
        this.metadata = metadata;
        setupLogging(cfg.logfile, cfg.loglevel);

    }

    @Override
    public void run() {
        init();
        connectToECS();
        if (!serverStatus.checkEqual(Constants.NO_CONNECTION)) {
            joinNetwork();
            readMetadata(ECSois);
            processRequest();
        }
    }

    /**
     * This function is used to process all the possible requests that the server
     * can receive from another server
     */
    private void processRequest() {
        while (!isInterrupted) {
            try {
                logger.info("Process Request");

                Socket s = serverSocket.accept();
                ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
                oos.flush();
                ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(s.getInputStream()));
                String[] command = read(ois);
                logger.info("New request received " + command[0]);
                switch (command[0]) {
                    case ("LOCK"):
                        sendData(command);
                        break;
                    case ("RECEIVE_DATA"):
                        receiveKVPairs(ois, oos);
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
                    default:
                        break;
                }
                if (shutdown) {
                    closeAll();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * This function is used to send the data to the new server that is joining
     * the network.
     * @param command
     */
    private void sendData(String[] command) {
        logger.info("sending file data");
        if (command.length == 5) {
            String ip = command[1];
            int port = Integer.parseInt(command[2]);
            String newHash = command[3];
            String successorStartIndex = command[4];
            serverStatus.setStatus(Constants.LOCKED);

            try {
                Socket s = new Socket(ip, port);
                ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
                oos.flush();
                ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(s.getInputStream()));
                logger.info("SEND DATA HASH: " + newHash);
                if (newHash != null) {
                    // Send data to new server
                    sendKVPairs(newHash, successorStartIndex, oos);
                    oos.close();
                    ois.close();
                    s.close();
                }
                logger.info("Data sent");
            } catch (IOException | ArrayIndexOutOfBoundsException e) {
                logger.severe("Error");
            }
        }
        else{
            handleError("Invalid Number of Parameters", Constants.INACTIVE);
        }
    }


    /**
     *
     * @param ois
     */
    private void receiveKVPairs(ObjectInputStream ois, ObjectOutputStream oos) {
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
     *
     */
    private void releaseLock() {
        logger.info("Releasing the lock");
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
            this.metadata.addAll(mtd);

            if (metadata == null) {
                handleError("An error occurred: you are connected with the same server", Constants.INACTIVE);
            } else if (metadata.size() == 1) {
                sendAck();
            }
            logger.info(metadata.toString());
        } catch (IOException | ClassNotFoundException e) {
            handleError("An error occurred while updating metadata", Constants.NO_CONNECTION);
        }
    }


    //********************************************************************************************/

    /**
     * This functions is the first function that is called when the thread is
     * started. It is used to create the server Socket that we need to communicate
     * with the others servers.
     *
     * @throws IOException
     */
    private void init() {
        logger.info("Init socket");

        Runtime.getRuntime().addShutdownHook(new Thread(this::defineShutDownHook));
        try {
            intraPort = getFreePort();
            this.serverSocket = new ServerSocket();
            if (myAddress != null) {
                SocketAddress address = new InetSocketAddress(myAddress, intraPort);
                serverSocket.bind(address);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    private void defineShutDownHook() {
        try {
            logger.info("Closing KVIntracommunication");

            if (serverStatus.checkEqual(Constants.INACTIVE)) {
                isInterrupted = true;
                closeAll();
            } else if (serverStatus.checkEqual(Constants.NO_CONNECTION)) {
                logger.log(Level.SEVERE, "An error occurred while connecting to the ECS");
            } else {
                shutdown = true;
                sendShutdown();
                isInterrupted = true;
                closeAll();
            }

        } catch (Exception ignored) {

        }
    }

    /**
     *
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
        }
    }

    /**
     *
     */
    private void sendShutdown() {
        connectToECS();

        logger.info("Sending shutdown");
        if (!serverStatus.checkEqual(Constants.NO_CONNECTION)) {
            try {
                String toSend = "shutdownServer " + this.myPort + " " + this.intraPort;
                long len = toSend.length();
                write(len, toSend, ECSoos);
            } catch (IOException e) {
                e.printStackTrace();
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
            handleError("An error occurred while connecting to the ECS", Constants.NO_CONNECTION);
        }

    }

    /**
     *
     * @param hostName
     * @param port
     * @return
     */
    private Socket openSocket(String hostName, int port) {
        try {
            return new Socket(hostName, port);
        } catch (IOException e) {
            handleError("An error occurred while connecting to the ECS", Constants.NO_CONNECTION);
            return null;
        }
    }

    /**
     *
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
            String toSend = "newServer " + this.myAddress + " " + this.myPort + " " + this.intraPort;
            long len = toSend.length();
            write(len, toSend, ECSoos);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * This function is used to send the ACK to the ECS.
     *
     * @throws IOException
     */
    private void sendAck()  {
        try {
            logger.info("send Ack");
            String toSend = "ACK";
            long len = toSend.length();
            if (ECSoos != null) {
                write(len, toSend, ECSoos);
            }
            closeAll();
            serverStatus.setStatus(Constants.ACTIVE);
            logger.info("Joining process completed!".toUpperCase());
        } catch(Exception e){
            handleError("An error occurred while sending the ACK", Constants.INACTIVE);
        }
    }

    /**
     *
     * @param len
     * @param toSend
     * @throws IOException
     */
    private void write(long len, String toSend, ObjectOutputStream oos) throws IOException {
        logger.info("Sending data; " + toSend);
        oos.writeLong(len);
        oos.flush();
        oos.writeUTF(toSend);
        oos.flush();
    }

    /**
     *
     * @param remoteIn
     * @return
     */
    private String[] read(ObjectInputStream remoteIn) {
        logger.info("Reading data from ObjectInputStream");
        long toRead;
        String command = null;
        String[] splitted = null;
        try {
            toRead = remoteIn.readLong();
            do {
                command = remoteIn.readUTF();
            } while (command.length() < toRead);
            splitted = command.split(" ");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return splitted;
    }



    /**
     *
     * @param hash
     * @param oos
     * @throws IOException
     */
    private void sendKVPairs(String hash, String successorStartIndex, ObjectOutputStream oos) throws IOException {
        data = new ArrayList<>();
        data = fileStorage.getRange(hash, successorStartIndex);
        logger.info("HASH: " + hash);
        logger.info("SUCCESSOR START: " + successorStartIndex);
        logger.info("SIZE DATA: " + data.size());
        oos.writeLong("RECEIVE_DATA".length());
        oos.flush();
        oos.writeUTF("RECEIVE_DATA");
        oos.flush();
        oos.writeObject(data);
        oos.flush();
    }
}

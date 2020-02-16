package de.tum.i13.server.kv;

import de.tum.i13.server.Cache.Cache;
import de.tum.i13.server.FileStorage.FileStorage;
import de.tum.i13.shared.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import static de.tum.i13.shared.LogSetup.setupLogging;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class KVStore implements KVStoreInterface {
    public static Logger logger = Logger.getLogger(KVStore.class.getName());

    private int cacheSize;
    private String displacementPolicy;
    private Metadata metadata;
    private Cache cache;
    private FileStorage fileStorage;
    private Path storagePath;
    private InetSocketAddress ecs;
    private ServerStatus serverStatus;
    private String myHash;
    private Path logFile;
    private String logLevel;
    private FileHandler fileHandler;
    private Thread kvIntra;
    private KVIntraCommunication kvIntraCommunication;
    private ConcurrentHashMap<String, Pair<Integer, Pair<String, Pair<String, String>>>> temporaryData;

    /**
     * set server status
     *
     * @param serverStatus
     */
    public void setServerStatus(ServerStatus serverStatus) {
        this.serverStatus = serverStatus;
    }

    public KVStore(Config cfg, Boolean test, ServerStatus serverStatus) throws IllegalArgumentException, NullPointerException {

        logFile = cfg.logfile;
        logLevel = cfg.loglevel;
        fileHandler = setupLogging(logFile, logLevel, logger);
        metadata = new Metadata(logger);
        this.serverStatus = serverStatus;
        this.cacheSize = cfg.cachesize;
        this.displacementPolicy = cfg.cachedisplacement;
        this.storagePath = cfg.dataDir;
        this.ecs = cfg.bootstrap;
        this.fileStorage = new FileStorage(this.storagePath, logger);
        this.myHash = Utility.computeHash(cfg.listenaddr, cfg.port);
        this.temporaryData = new ConcurrentHashMap<>();
        // Restore previous data
        if (!test) {
            try {
                this.fileStorage.restore();
            } catch (FileNotFoundException ignored) {
            }
        }
        try {
            cache = new Cache(this.cacheSize, this.displacementPolicy, logger);
        } catch (NullPointerException e) {
            throw new NullPointerException();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException();
        }

        // Send a request to the ECS to enter in the network
        kvIntraCommunication = new KVIntraCommunication(cfg, this.serverStatus, metadata, fileStorage, logger, cache);
        kvIntra = new Thread(kvIntraCommunication);
        kvIntra.start();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        timer.cancel();
                        timer.purge();
                    } catch (Exception e) {

                    }

                }));
                if (temporaryData.size() > 0) {
                    ArrayList<Pair<Integer, Pair<String, Pair<String, String>>>> array = new ArrayList<>();
                    temporaryData.forEach((v, p) -> {
                                array.add(new Pair<>(Constants.NUM_REPLICAS, p.getSecond()));
                                temporaryData.remove(v);
                            }
                    );

                    Map.Entry<String, DataMap> successor = metadata.getMySuccessor(myHash);
                    Socket s = null;
                    try {
                        s = new Socket(successor.getValue().getIp(), successor.getValue().getIntraPort());
                        ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
                        Common.sendKVPairs(array, oos, "RECEIVE_REPLICA_SHUTDOWN");
                    } catch (IOException e) {

                    }
                }
            }
        }, Constants.EVENTUAL_HASHING_TIMEOUT, Constants.EVENTUAL_HASHING_TIMEOUT);


    }

    /**
     * close kvstore
     */
    public void close() {
        logger.info("Closing KVStore");
        kvIntraCommunication.close();


        logger.info("Closing log file");
        fileHandler.close();
    }

    public Pair<Path, String> getLog() {
        return new Pair<>(logFile, logLevel);
    }

    @Override
    /**
     *  This function puts the <key, value> pair into the hashmap.
     *
     * @param   key   the key that we want to insert
     * @param   value the value that we want to insert
     * @return 0 if we added the pair to the hashmap
     *          -1 in case of error
     *          1 if we update the key,value pair
     *          2 if we deleted the pair
     *          3 if the server is not responsible
     *          4 if the server is stopped
     */
    public int put(String key, String value, Object... password) {
        if (serverStatus.checkEqual(Constants.ACTIVE)) {
            if (metadata.isResponsible(this.myHash, Utility.computeHash(key))) {
                int ret = -1;

                // Add on disk
                /*if(this.fullConsistency){

                    if(isUpdate(key){
                        // If we are updating the value and if we want the full consistency we do the 2 phase commit.
                        ret = start2PhaseCommit(key, value, password);
                        return ret;
                    }
                }*/

                if (password.length > 0) {
                    logger.info("PutWithPassword: " + key + " " + value + " " + password[0]);
                    try {
                        ret = fileStorage.put(key, value, password);
                    } catch (InvalidPasswordException e) {
                        return Constants.INVALID_PASSWORD;
                    }
                } else {
                    try {
                        ret = fileStorage.put(key, value);
                    } catch (InvalidPasswordException e) {
                        return Constants.INVALID_PASSWORD;
                    }
                }

                if (ret != Constants.ERROR) { // We don't have any error
                    if (password.length > 0) {
                        try {
                            cache.put(key, value, password);
                        } catch (InvalidPasswordException e) {
                            return Constants.INVALID_PASSWORD;
                        }
                    } else {
                        try {
                            cache.put(key, value);
                        } catch (InvalidPasswordException e) {
                            return Constants.INVALID_PASSWORD;
                        }
                    }
                    if (ret == Constants.PUT_UPDATE) {
                        if (password.length > 0)
                            temporaryData.put(key, new Pair<>(Constants.UPDATE_KV, new Pair<>(key, new Pair<>(value, (String) password[0]))));
                        else {
                            Pair<Integer, Pair<String, Pair<String, String>>> s = temporaryData.put(key, new Pair<>(Constants.UPDATE_KV, new Pair<>(key, new Pair<>(value, null))));
                        }
                    }
                    if (metadata.size() >= 3 && ret != Constants.PUT_UPDATE) {
                        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> Common.sendReplica(key, value, myHash, Constants.NUM_REPLICAS, metadata));
                    }
                    return ret;
                }
            }

            return Constants.SERVER_NOT_RESPONSIBLE;
        } else if (serverStatus.checkEqual(Constants.LOCKED)) {
            return Constants.WRITE_LOCK;
        }
        return Constants.SERVER_STOPPED;
    }

    private boolean checkOperation(String key) {
        return fileStorage.isUpdate(key);
    }

/*
    private int start2PhaseCommit(String key, String value, Object ... password) {
        ArrayList<Boolean> replies = null;
        try {
            replies = preparePhase();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(checkCommit(replies)){
            commitPhase();
        } else{
            abortPhase();
        }
    }

    private ArrayList<Boolean> preparePhase() throws IOException {
        ArrayList<SocketAddress> successors = metadata.getSuccessors(this.myHash);
        ArrayList<Pair<Socket, Pair<ObjectInputStream, ObjectOutputStream>>>  openSockets = openSocket(successors);

    }
*/


    /**
     * Gets a key value pair from the kvstore
     *
     * @param key the key that we are searching for
     * @return server_stopped: the server is currently stopped
     * server_write_lock: the server currently does not allow anyone to write
     * server_not_responsible: the server is not responible for this particular key
     * null: get error
     * else: success
     */
    @Override
    public String get(String key, Object... psw) {
        logger.info("Server Status: " + serverStatus.getStatus());
        if (serverStatus.checkEqual(Constants.ACTIVE)) {
            logger.info("Active Server, processing request");
            String hashData = Utility.computeHash(key);

            if (metadata.size() >= Constants.NUM_REPLICAS + 1) {
                if (metadata.isReplica(myHash, hashData)) {
                    try {
                        return searchDataAndReturn(key, psw);
                    } catch (InvalidPasswordException e) {
                        return Constants.INVALIDPASSWORD;
                    }
                } else {
                    if (metadata.isResponsible(myHash, hashData)) {
                        try {
                            return searchDataAndReturn(key, psw);
                        } catch (InvalidPasswordException e) {
                            return Constants.INVALIDPASSWORD;
                        }
                    }
                }
            } else {
                if (metadata.isResponsible(myHash, hashData)) {
                    try {
                        return searchDataAndReturn(key, psw);
                    } catch (InvalidPasswordException e) {
                        return Constants.INVALIDPASSWORD;
                    }
                }
            }
            return "server_not_responsible";
        } else if (serverStatus.checkEqual(Constants.LOCKED)) {
            return "server_write_lock";
        }
        logger.info("Server is stopped");
        return "server_stopped";
    }

    /**
     * delete data
     *
     * @param key the key that we want to remove
     * @return 1: delete success
     * 0: delete error, data not found
     * 3: the server is currently stopped
     * 4: the server currently does not allow anyone to write
     * 2: the server is not responible for this particular key
     */
    @Override
    public int delete(String key, Object... pwd) throws InvalidPasswordException {
        if (serverStatus.checkEqual(Constants.ACTIVE)) {
            if (metadata.isResponsible(myHash, Utility.computeHash(key))) {
                // Remove from disk
                Pair<String, String> deleted = null;
                if (pwd.length == 0)
                    deleted = fileStorage.remove(key);
                else
                    deleted = fileStorage.remove(key, pwd);

                if (deleted != null) {
                    if (deleted.getFirst() != null) {
                        logger.info("Data deleted from disk");
                        // Remove from cache (if it is present)
                        if (pwd.length == 0)
                            cache.remove(key);
                        else
                            cache.remove(key, pwd);
                        temporaryData.put(key, new Pair<>(Constants.DELETED_KV, new Pair<>(key, new Pair<>(null, deleted.getSecond()))));
                        return Constants.DELETE_OK;
                    }
                }


                return Constants.DELETE_ERROR;
            }
            return Constants.DELETE_NOT_RESP;
        } else if (serverStatus.checkEqual(Constants.LOCKED)) {
            return Constants.DELETE_WRITE_LOCK;
        }
        return Constants.DELETE_STOPPED;
    }



    /*************************************************************************************/

    /**
     * @param key
     * @return search given key and return it. If not stored return null
     */
    private String searchDataAndReturn(String key, Object... pwd) throws InvalidPasswordException {
        // Search in the cache
        String retValue = null;
        String password = null;

        retValue = cache.get(key, pwd);
        logger.info("get from cache: " + retValue);

        if (retValue == null) {
            // Search on disk
            retValue = fileStorage.get(key, pwd);
            logger.info("Get from filestorage: " + retValue);
            // If I found the data on disk I store the data also in the cache
            if (retValue != null) {
                cache.put(key, retValue, pwd);
            }
        }
        return retValue;
    }

    /**
     * @param hash the hash of the file that we are looking for
     * @return a string with the adress and ip in the following format: "adress ip"
     */
    @Override
    public String getKeyRange(String hash) {
        Pair<String, Integer> pair = metadata.getResponsible(hash); //get the responsible server with its port and adress from the metadata
        StringBuilder server = new StringBuilder();
        server.append(pair.getFirst());
        server.append(" ");
        server.append(pair.getSecond().toString());
        return server.toString();
    }

    /**
     * @return keyrange in string format
     */
    public String getKeyRange() {

        if (metadata != null) {
            StringBuilder response = new StringBuilder();
            response.append("keyrange_success");
            response.append(" ");
            response.append(metadata.toString());
            response.delete(response.length() - 2, response.length());
            return response.toString();
        }
        return null;
    }

    /**
     * @return key range of replicas in string format
     */
    public String getKeyRangeReplicas() {

        if (metadata != null) {
            StringBuilder response = new StringBuilder();
            response.append("keyrange_read_success");
            response.append(" ");
            response.append(metadata.toStringReplicas());
            response.delete(response.length() - 2, response.length());
            return response.toString();
        }
        return null;
    }

    /**
     * @return the metadata in string format
     */
    public String getMetadata() {
        return metadata.toString();
    }

    /**
     * This function is called when we want to receive informations about replicas
     *
     * @return replica metadata
     */
    public String getReplicaMetadata() {
        return metadata.toStringReplicas();
    }

    /**
     * @return Logger
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * This function is used only in the tests
     */
    public void simulateSIGKILL() {
        kvIntraCommunication.simulateSIGKILL();
    }


}

package de.tum.i13.server.kv;

import de.tum.i13.server.Cache.Cache;
import de.tum.i13.server.FileStorage.FileStorage;
import de.tum.i13.shared.*;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
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
    private String myAddress;
    private ServerStatus serverStatus;
    private String myHash;
    private Path logFile;
    private String logLevel;
    private FileHandler fileHandler;
    private Thread kvIntra;
    private KVIntraCommunication kvIntraCommunication;
    // DELETED_KV = -1, NEW_KV = 0, UPDATE_KV = 1;
    private ConcurrentHashMap<String, Pair<Integer, Pair<String, String>>> temporaryData;

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
        this.myAddress = cfg.listenaddr;
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

                if (temporaryData.size() > 0) {
                    ArrayList<Pair<Integer, Pair<String, String>>> array = new ArrayList<>();
                    temporaryData.forEach((v, p) ->
                            array.add(new Pair<Integer, Pair<String, String>>(Constants.NUM_REPLICAS, p.getSecond()))
                    );
                    Map.Entry<String, DataMap> successor = metadata.getMySuccessor(myHash);
                    Socket s = null;
                    try {
                        s = new Socket(successor.getValue().getIp(), successor.getValue().getIntraPort());
                        ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
                        Common.sendKVPairs(array, oos, "RECEIVE_REPLICA_SHUTDOWN");
                        temporaryData.clear();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }, Constants.EVENTUAL_HASHING_TIMEOUT);


    }

    /**
     * close kvstore
     */
    public void close() {
        logger.info("Closing KVStore");
        kvIntraCommunication.close();
        //kvIntra.interrupt();
        /* while (!serverStatus.checkEqual(String.valueOf(Constants.SHUTDOWN))) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/

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
    public int put(String key, String value) {
        if (serverStatus.checkEqual(Constants.ACTIVE)) {
            if (metadata.isResponsible(this.myHash, Utility.computeHash(key))) {
                // Add on disk
                int ret = fileStorage.put(key, value);
                if (ret != Constants.ERROR) { // We don't have any error
                    cache.put(key, value);
                    if (ret == Constants.PUT_UPDATE) {
                        temporaryData.put(key, new Pair<>(Constants.UPDATE_KV, new Pair<>(key, value)));
                    }
                    if (metadata.size() >= 3) {
                        // We add the data to fileStorage and to cache and then we start a completableFuture Task
                        // so that the client will immediately receive the ack and can terminate the put request
                        // and in the meantime the server will send the newly added k,v pair to its successor.
                        // From the responsible server we send the data only to the first successor (first replica) then
                        // the first replica will forward the k,v pair to its successor so that we can balance the work
                        // on the two servers.
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
    public String get(String key) {
        if (serverStatus.checkEqual(Constants.ACTIVE)) {
            String hashData = Utility.computeHash(key);

            if (metadata.size() >= Constants.NUM_REPLICAS + 1) {
                if (metadata.isReplica(myHash, hashData)) {
                    return searchDataAndReturn(key);
                } else {
                    if (metadata.isResponsible(myHash, hashData)) {
                        return searchDataAndReturn(key);
                    }
                }
            } else {
                if (metadata.isResponsible(myHash, hashData)) {
                    return searchDataAndReturn(key);
                }
            }
            return "server_not_responsible";
        } else if (serverStatus.checkEqual(Constants.LOCKED)) {
            return "server_write_lock";
        }
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
    public int delete(String key) {
        if (serverStatus.checkEqual(Constants.ACTIVE)) {
            if (metadata.isResponsible(myHash, Utility.computeHash(key))) {
                // Remove from disk
                if (fileStorage.remove(key) != null) {
                    logger.info("Data deleted from disk");
                    // Remove from cache (if it is present)
                    cache.remove(key);
                    temporaryData.put(key, new Pair<>(Constants.DELETED_KV, new Pair<>(key, null)));
                    return Constants.DELETE_OK;
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
    private String searchDataAndReturn(String key) {
        // Search in the cache
        String retValue = cache.get(key);
        if (retValue == null) {
            // Search on disk
            retValue = fileStorage.get(key);
            // If I found the data on disk I store the data also in the cache
            if (retValue != null) {
                cache.put(key, retValue);
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

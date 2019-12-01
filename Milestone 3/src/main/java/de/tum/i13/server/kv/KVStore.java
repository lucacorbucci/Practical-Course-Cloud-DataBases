package de.tum.i13.server.kv;

import de.tum.i13.server.Cache.Cache;
import de.tum.i13.server.FileStorage.FileStorage;
import de.tum.i13.server.nio.StartNioServer;
import de.tum.i13.shared.*;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import static de.tum.i13.shared.LogSetup.setupLogging;
import static de.tum.i13.shared.Utility.byteToHex;

/**
 *
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class KVStore implements KVStoreInterface{
    public static Logger logger = Logger.getLogger(KVStoreInterface.class.getName());

    private int cacheSize;
    private String displacementPolicy;
    private Metadata metadata = new Metadata();
    private Cache cache;
    private FileStorage fileStorage;
    private Path storagePath;
    private InetSocketAddress ecs;
    private String myAddress;
    private ServerStatus serverStatus;
    private String myHash;

    public void setServerStatus(ServerStatus serverStatus) {
        this.serverStatus = serverStatus;
    }

    public KVStore(Config cfg, Boolean test, ServerStatus serverStatus) throws IllegalArgumentException, NullPointerException{
        setupLogging("OFF");
        this.serverStatus = serverStatus;
        this.cacheSize = cfg.cachesize;
        this.myAddress = cfg.listenaddr;
        this.displacementPolicy = cfg.cachedisplacement;
        this.storagePath = cfg.dataDir;
        this.ecs = cfg.bootstrap;
        this.fileStorage = new FileStorage(this.storagePath);
        this.myHash = computeHash(cfg.listenaddr, cfg.port);
        // Restore previous data
        if(!test)
            this.fileStorage.restore();
        // Send a request to the ECS to enter in the network

        Thread thread = new Thread((new KVIntraCommunication(cfg, this.serverStatus, metadata, fileStorage)));
        thread.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("closing server");
        }));

        try{
            cache = new Cache(this.cacheSize, this.displacementPolicy);
        } catch(NullPointerException e) {
            throw new NullPointerException();
        } catch(IllegalArgumentException e){
            throw new IllegalArgumentException();
        }
    }


    @Override
    /**
     *  This function puts the <key, value> pair into the hashmap.
     *
     * @param   key   the key that we want to insert
     * @param   value the value that we want to insert
     * @return  0 if we added the pair to the hashmap
     *          -1 in case of error
     *          1 if we update the key,value pair
     *          2 if we deleted the pair
     *          3 if the server is not responsible
     *          4 if the server is stopped
     */
    public int put(String key, String value){

        if (serverStatus.checkEqual(Constants.ACTIVE)){

            if (metadata.isResponsible(this.myHash, computeHash(key))){
                // Add on disk
                int ret = fileStorage.put(key, value);
                if(ret != Constants.ERROR){ // We don't have any error
                    if(ret != Constants.DELETED)
                        // update or new pair
                        cache.put(key, value);
                    // else we deleted the pair
                    return ret;
                }
            }
            return Constants.SERVER_NOT_RESPONSIBLE;
        } else if(serverStatus.checkEqual(Constants.LOCKED)){
            return Constants.WRITE_LOCK;
        }
        return Constants.SERVER_STOPPED;
    }

    /**
     * Gets a key value pair from the kvstore
     * @param   key the key that we are searching for
     * @return  server_stopped: the server is currently stopped
     *          server_write_lock: the server currently does not allow anyone to write
     *          server_not_responsible: the server is not responible for this particular key
     *          null: get error
     *          else: success
     */
    @Override
    public String get(String key) {
        logger.info("-----------");
        logger.info(key);
        logger.info(computeHash(key));
        logger.info(myHash);
        logger.info("-----------");

        if (serverStatus.checkEqual(Constants.ACTIVE)){

            if (metadata.isResponsible(myHash, computeHash(key))) {
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
            return "server_not_responsible";
        } else if (serverStatus.checkEqual(Constants.LOCKED)){
            return "server_write_lock";
        }
        return "server_stopped";
    }

    /**
     *delete data
     * @param   key   the key that we want to remove
     * @return
     *          1: delete success
     *          0: delete error, data not found
     *          3: the server is currently stopped
     *          4: the server currently does not allow anyone to write
     *          2: the server is not responible for this particular key
     */
    @Override
    public int delete(String key) {
        if (serverStatus.checkEqual(Constants.ACTIVE)){
            if (metadata.isResponsible(myHash, computeHash(key))){
                // Remove from disk
                if(fileStorage.remove(key)!= null){
                    StartNioServer.logger.info("Data deleted from disk");
                    // Remove from cache (if it is present)
                    cache.remove(key);
                    return Constants.DELETE_OK;
                }
                return Constants.DELETE_ERROR;
            }
            return Constants.DELETE_NOT_RESP;
        }else if(serverStatus.checkEqual(Constants.LOCKED)){
            return Constants.DELETE_WRITE_LOCK;
        }
        return Constants.DELETE_STOPPED;
    }



    /**
     *
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

    public String getKeyRange(){

        if (metadata != null){
            StringBuilder response = new StringBuilder();
            response.append("keyrange_success");
            response.append(" ");
            response.append(metadata.toString());
            return response.toString();
        }
        return null;
    }

    public String getMetadata() {
        return metadata.toString();
    }


    private String computeHash(String key){
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(key.getBytes());
            return byteToHex(messageDigest);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *
     * @param ip
     * @param port
     * @return
     */
    private String computeHash(String ip, int port){
        MessageDigest md = null;
        try {
            StringBuilder str = new StringBuilder();
            str.append(ip);
            str.append(port);
            String toHash = str.toString();
            md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(toHash.getBytes());

            return byteToHex(messageDigest);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}

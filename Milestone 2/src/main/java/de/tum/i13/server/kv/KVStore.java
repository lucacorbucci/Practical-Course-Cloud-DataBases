package de.tum.i13.server.kv;

import de.tum.i13.server.Cache.Cache;
import de.tum.i13.server.FileStorage.FileStorage;
import de.tum.i13.server.nio.StartNioServer;

import java.nio.file.Path;

/**
 *
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class KVStore implements KVStoreInterface{

    private int cacheSize;
    private String displacementPolicy;
    Cache cache;
    FileStorage fileStorage;
    Path storagePath;

    public KVStore(int cacheSize, String displacementPolicy, Path storagePath, Boolean test) throws IllegalArgumentException, NullPointerException{
        this.cacheSize = cacheSize;
        this.displacementPolicy = displacementPolicy;
        this.storagePath = storagePath;
        this.fileStorage = new FileStorage(this.storagePath);
        // Restore previous data
        if(!test)
            this.fileStorage.restore();
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
     */
    public int put(String key, String value) {
        // Add on disk
        int ret = fileStorage.put(key, value);
        if(ret != -1){ // We don't have any error
            if(ret != 2)
                // update or new pair
                cache.put(key, value);
            // else we deleted the pair

            return ret;
        }
        else
            return -1;
    }

    @Override
    public String get(String key) {
        // Search in the cache
        String retValue = cache.get(key);
        if(retValue == null){
            // Search on disk
            retValue = fileStorage.get(key);
            // If I found the data on disk I store the data also in the cache
            if(retValue != null){
                cache.put(key, retValue);
            }
        }

        return retValue;
    }

    @Override
    public int delete(String key) {
        // Remove from disk
        if(fileStorage.remove(key)!= null){
            StartNioServer.logger.info("Data deleted from disk");
            // Remove from cache (if it is present)
            cache.remove(key);
            return 1;
        }
        return 0;
    }

}

package de.tum.i13.server.FileStorage;

import de.tum.i13.server.nio.StartNioServer;
import de.tum.i13.shared.Constants;

import java.io.File;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;
/**
 * This class is used to store the data on disk.
 * We use a TreeMap to store the pairs <hash, Filemap> where the filemap
 * is basically a "pointer" to the data stored on disk. For each of the keys
 * that are stored in this file we compute the hash and we store the highest hash in the Treemap.
 * The idea is similar to the idea of the ring of consistent hashing because when we want to add a new pair
 * we can find the file where we have to store this pair using the hash:
 * - We search into the Treemap the hash of the key that we want to add
 * - We found the first pair<HASH, FILEMAP> in the TreeMap
 *   where the hash(new key) < HASH
 * - We add the new pair to this FILEMAP
 *
 *
 * We do the same thing to find a value, when we have the hash of the key we search for
 * the pair in the treeMap and then we search the key in the corresponding FileMap.
 *
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class FileStorage implements FileStorageInterface {
    private TreeMap<Integer, FileMap> map;
    Path path;

    public FileStorage(Path path){
        this.path = path;
        this.map = new TreeMap<>();
    }


    private Boolean checkExtension(String filename) {
        return (filename.substring(filename.lastIndexOf(".") + 1).equals("txt"));
    }

    /**
     * This function is used to restore the data in the database
     * when we relaunch the server after shutdown.
     */
    public void restore(){
        File[] files = new File(path.toAbsolutePath().toString() +"/").listFiles();
        for (File f : files){
            if(checkExtension(f.getName())){
                FileMap fm = new FileMap(path.toAbsolutePath().toString() + "/" + f.getName(), Constants.MAX_FILE_SIZE, path);
                map.put(fm.getHash(), fm);
            }
        }

    }

    @Override
    /**
     * This function adds a new pair to the database stored on disk.
     * If the key already exists in the database, the value is updated with the
     * one that we pass as parameter.
     * If the value == null the key,value pair is removed from disk
     *
     * @param key the key that we have to insert in our database
     * @param value the value that we have to insert in our database
     * @return -1 in case of error
     *         0 if we added a new key, value pair
     *         1 if we updated the key, value
     *         2 if we deleted the key
     */
    public int put(String key, String value) {
        int hash = key.hashCode();
        if(value == null){
            if(this.remove(key) == null)
                return -1;
            else
                return 2;
        }

        // First pair in the map
        if(isEmpty(map)){
            FileMap fm = new FileMap(Constants.MAX_FILE_SIZE, key, value, this.path);
            map.put(hash, fm);
            return 0;
        }
        else{
            SortedMap<Integer, FileMap> partialMap = map.tailMap(hash);
            FileMap fm;
            int oldHash;
            if(isEmpty(partialMap)){
                oldHash = map.firstKey();
                fm = map.get(oldHash);
            }
            else{
                oldHash = partialMap.firstKey();
                fm = partialMap.get(oldHash);
            }
            // Add the pair to the FileMap and check if we splitted the file
            rebalanceReturn rr = fm.addPair(key,value);

            // If we have splitted the file we have to add the "pointer" of the new file
            // to the treemap
            if(rr != null){
                if(rr.getFirstHash() == null && rr.getLastHash() == null && rr.getFm() == null){
                    return 1;
                }
                else if(rr.getFirstHash() == null && rr.getLastHash() == null){
                    map.put(hash, rr.getFm());
                }
                else{
                    int lastHash = rr.getLastHash();
                    int firstHash = rr.getFirstHash();

                    if(lastHash != oldHash){
                        FileMap fmOld = map.remove(oldHash);
                        map.put(lastHash, fmOld);
                    }
                    map.put(firstHash, rr.getFm());

                }
            }
            return 0;
        }

    }

    @Override
    /**
     * This function returns the value associated with the parameter key
     *
     * @param key the key whose the associated value has to be returned
     * @return the value associated with key, null if key is not in the database
     */
    public String get(String key) {
        int hash = key.hashCode();
        String retValue = null;
        SortedMap<Integer, FileMap> partialMap = map.tailMap(hash);
        // First element check
        try{
            if(!isEmpty(partialMap)){
                Integer firstKey = partialMap.firstKey();
                retValue = partialMap.get(firstKey).getValue(key);
            } else{
                Integer firstKey = map.firstKey();
                retValue = map.get(firstKey).getValue(key);
            }
        } catch(NoSuchElementException e){
            retValue = null;
        }
        return retValue;
    }

    @Override
    /**
     * This function removes the key,value pair from the database stored on disk.
     *
     * @param key the key that we want to remove
     * @return null if key is not present in the database else the value previously associated
     *         with key
     */
    public String remove(String key) {
        int hash = key.hashCode();
        String retValue = null;
        SortedMap<Integer, FileMap> partialMap = map.tailMap(hash);
        try{
            if(!isEmpty(partialMap)){
                Integer firstKey = partialMap.firstKey();
                retValue = partialMap.get(firstKey).remove(key);
            }
            else{
                Integer firstKey = map.firstKey();
                retValue = map.get(firstKey).remove(key);
            }
        } catch (NoSuchElementException e){
            StartNioServer.logger.info("Key not in db");
            retValue = null;
        }

        return retValue;
    }


    private boolean isEmpty(SortedMap<Integer, FileMap> tMap){
        return tMap.size() == 0;
    }
}

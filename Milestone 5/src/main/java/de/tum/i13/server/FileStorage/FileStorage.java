package de.tum.i13.server.FileStorage;

import de.tum.i13.shared.Constants;
import de.tum.i13.shared.InvalidPasswordException;
import de.tum.i13.shared.Pair;
import de.tum.i13.shared.Utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;

import static de.tum.i13.shared.Utility.byteToHex;

/**
 * This class is used to store the data on disk.
 * We use a TreeMap to store the pairs <hash, Filemap> where the filemap
 * is basically a "pointer" to the data stored on disk. For each of the keys
 * that are stored in this file we compute the hash and we store the highest hash in the Treemap.
 * The idea is similar to the idea of the ring of consistent hashing because when we want to add a new pair
 * we can find the file where we have to store this pair using the hash:
 * - We search into the Treemap the hash of the key that we want to add
 * - We found the first pair<HASH, FILEMAP> in the TreeMap
 * where the hash(new key) < HASH
 * - We add the new pair to this FILEMAP
 * <p>
 * <p>
 * We do the same thing to find a value, when we have the hash of the key we search for
 * the pair in the treeMap and then we search the key in the corresponding FileMap.
 *
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class FileStorage implements FileStorageInterface {
    private TreeMap<String, FileMap> map;
    private Path path;
    private Logger logger;

    public FileStorage(Path path, Logger logger) {
        this.path = path;
        this.map = new TreeMap<>();
        this.logger = logger;
    }


    public void clear() {
        File[] files = new File(path.toAbsolutePath().toString() + "/").listFiles();
        for (File f : files) {
            if (checkExtension(f.getName())) {
                f.delete();
            }
        }
    }


    private Boolean checkExtension(String filename) {
        return (filename.substring(filename.lastIndexOf(".") + 1).equals("txt"));
    }

    /**
     * This function is used to restore the data in the database
     * when we relaunch the server after shutdown.
     */
    public void restore() throws FileNotFoundException {
        File[] files = new File(path.toAbsolutePath().toString() + "/").listFiles();
        for (File f : files) {
            if (checkExtension(f.getName())) {
                FileMap fm = new FileMap(path.toAbsolutePath().toString() + "/" + f.getName(), Constants.MAX_FILE_SIZE, path);
                map.put(fm.getHash(), fm);
            }
        }

    }


    /**
     * This function returns an arrayList of pairs. Each pair contains a key
     * and a value. We filter the key and we return only the keys that are
     * smaller than the hash that we passed as parameter.
     *
     * @param end The hash that we compare with the strings in our map
     * @return An ArrayList of pair<String,String>
     */
    public ArrayList<Pair<String, Pair<String, String>>> getRange(String end, String start) {
        if (map != null) {
            logger.info("GET RANGE");
            ArrayList<Pair<String, Pair<String, String>>> kvstore = new ArrayList<>();
            map.forEach((s, file) -> {
                try {
                    HashMap<String, Pair<String, String>> kvalue = FileMap.read(file.getFileName());
                    kvalue.forEach((k, v) -> {
                        logger.info("KEY " + k);
                        String hashedKey = computeHash(k);

                        if (hashedKey != null) {
                            if (isInRange(hashedKey, start, end))
                                kvstore.add(new Pair<String, Pair<String, String>>(k, v));
                        }

                    });
                } catch (FileNotFoundException e) {
                    logger.info("File Deleted, I don't have any data to send");
                }

            });
            return kvstore;
        } else {
            logger.info("GET RANGE NULL");
        }

        return new ArrayList<>();
    }

    public Pair<ArrayList<Pair<String, Pair<String, String>>>, ArrayList<Pair<String, Pair<String, String>>>> getReplicas(Pair<String, String> predecessorHash, Pair<String, String> predPredHash) {
        ArrayList<Pair<String, Pair<String, String>>> predecessor = new ArrayList<>();
        ArrayList<Pair<String, Pair<String, String>>> predPred = new ArrayList<>();
        map.forEach((s, file) -> {
            HashMap<String, Pair<String, String>> kvalue = null;
            try {
                kvalue = FileMap.read(file.getFileName());
                kvalue.forEach((k, v) -> {
                    String hashedKey = computeHash(k);
                    if (isInRange(hashedKey, predecessorHash.getFirst(), predecessorHash.getSecond())) {
                        predecessor.add(new Pair<>(k, v));
                    } else if (isInRange(hashedKey, predPredHash.getFirst(), predPredHash.getSecond())) {
                        predPred.add(new Pair<>(k, v));
                    }
                });
            } catch (FileNotFoundException e) {
                logger.warning("File not found");
            }

        });
        return new Pair<>(predecessor, predPred);
    }

    private boolean isInRange(String hashedKey, String start, String end) {
        if (Utility.compareHex(start, end) == 1) {
            return (Utility.compareHex(hashedKey, start) >= 0 && Utility.compareHex(hashedKey, Constants.HEX_END_INDEX) <= 0)
                    || (Utility.compareHex(hashedKey, Constants.HEX_START_INDEX) >= 0 && Utility.compareHex(hashedKey, end) <= 0);
        } else return Utility.compareHex(hashedKey, start) >= 0 && Utility.compareHex(hashedKey, end) <= 0;

    }


    public ArrayList<Pair<String, Pair<String, String>>> getAll() {
        ArrayList<Pair<String, Pair<String, String>>> kvstore = new ArrayList<>();
        map.forEach((s, file) -> {
            HashMap<String, Pair<String, String>> kvalue = null;
            try {
                kvalue = FileMap.read(file.getFileName());
                kvalue.forEach((k, v) -> {
                    kvstore.add(new Pair<String, Pair<String, String>>(k, v));
                });
            } catch (FileNotFoundException e) {
                logger.warning("File Not found");
            }

        });
        return kvstore;
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
    public int put(String key, String v, Object... p) throws InvalidPasswordException {
        String hash = computeHash(key);
        Pair<String, String> value;

        if (p.length > 0 && p[0] != null)
            value = new Pair<String, String>(v, (String) p[0]);
        else
            value = new Pair<String, String>(v, null);

        // First pair in the map
        if (isEmpty(map)) {
            return addNewFile(key, value, hash);
        } else {
            // We are looking for the first hash bigger than our hash
            SortedMap<String, FileMap> partialMap = map.tailMap(hash);
            FileMap fm;
            String oldHash;
            // If we don't have a file with the hash bigger than our hash we insert the new
            // kv pair in a new file.
            if (isEmpty(partialMap)) {
                return addNewFile(key, value, hash);
            }
            // Otherwise we get the file and we add the new kv pair to this file
            else {
                oldHash = partialMap.firstKey();
                fm = partialMap.get(oldHash);
                // Add the pair to the FileMap and check if we splitted the file
                rebalanceReturn rr = null;
                try {
                    rr = fm.addPair(key, value);
                } catch (InvalidPasswordException e) {
                    throw new InvalidPasswordException();
                }

                // If we have splitted the file we have to add the "pointer" of the new file
                // to the treemap
                if (rr != null) {
                    if (rr.getFirstHash() == null && rr.getLastHash() == null && rr.getFm() == null) {
                        return Constants.PUT_UPDATE;
                    } else if (rr.getFirstHash() == null && rr.getLastHash() == null) {
                        synchronized (map) {
                            map.put(hash, rr.getFm());
                        }

                    } else {
                        String lastHash = rr.getLastHash();
                        String firstHash = rr.getFirstHash();

                        if (lastHash != oldHash) {
                            FileMap fmOld = map.remove(oldHash);
                            synchronized (map) {
                                map.put(lastHash, fmOld);
                            }
                        }
                        synchronized (map) {
                            map.put(firstHash, rr.getFm());
                        }
                    }
                }
            }

            return Constants.PUT_SUCCESS;
        }

    }

    private int addNewFile(String key, Pair<String, String> value, String hash) {
        FileMap fm = new FileMap(Constants.MAX_FILE_SIZE, key, value, this.path);
        synchronized (map) {
            map.put(hash, fm);
        }
        return 0;
    }

    @Override
    /**
     * This function returns the value associated with the parameter key
     *
     * @param key the key whose the associated value has to be returned
     * @return the value associated with key, null if key is not in the database
     */
    public String get(String key, Object... p) {
        String hash = computeHash(key);
        String retValue = null;
        SortedMap<String, FileMap> partialMap = map.tailMap(hash);
        // First element check
        try {
            if (!isEmpty(partialMap)) {
                String firstKey = partialMap.firstKey();
                if (p.length > 0)
                    retValue = partialMap.get(firstKey).getValue(key, (p[0]));
                else
                    retValue = partialMap.get(firstKey).getValue(key);
            }
        } catch (NoSuchElementException | NullPointerException | FileNotFoundException e) {
            retValue = null;

        }
        return retValue;
    }

    public boolean isUpdate(String key) {
        String hash = computeHash(key);
        boolean retValue = false;
        SortedMap<String, FileMap> partialMap = map.tailMap(hash);
        // First element check
        try {
            if (!isEmpty(partialMap)) {
                String firstKey = partialMap.firstKey();
                retValue = partialMap.get(firstKey).isPresent(key);
            }
        } catch (NoSuchElementException | FileNotFoundException e) {
            retValue = false;
        }
        return retValue;
    }

    public void checkAndDelete(String key) {
        String hash = computeHash(key);
        SortedMap<String, FileMap> partialMap = map.tailMap(hash);
        // First element check
        try {
            if (!isEmpty(partialMap)) {
                String firstKey = partialMap.firstKey();
                partialMap.get(firstKey).remove(key);
            }
        } catch (NoSuchElementException | FileNotFoundException e) {

        } catch (InvalidPasswordException e) {
            e.printStackTrace();
        }

    }

    @Override
    /**
     * This function removes the key,value pair from the database stored on disk.
     *
     * @param key the key that we want to remove
     * @return null if key is not present in the database else the value previously associated
     *         with key
     */
    public Pair<String, String> remove(String key, Object... p) throws InvalidPasswordException {
        String hash = computeHash(key);
        Pair<String, String> retValue = null;
        SortedMap<String, FileMap> partialMap = map.tailMap(hash);
        try {
            if (!isEmpty(partialMap)) {
                String firstKey = partialMap.firstKey();
                synchronized (map) {
                    if (p.length == 0)
                        retValue = new Pair<>(partialMap.get(firstKey).remove(key), null);
                    else
                        retValue = new Pair<>(partialMap.get(firstKey).remove(key, p[0]), (String) p[0]);
                }
            }

        } catch (NoSuchElementException | FileNotFoundException e) {
            logger.info("Key not in db");
            retValue = null;
        } catch (InvalidPasswordException e) {
            throw new InvalidPasswordException();
        }

        return retValue;
    }

    private String computeHash(String key) {
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


    private boolean isEmpty(SortedMap<String, FileMap> tMap) {
        return tMap.size() == 0;
    }


}

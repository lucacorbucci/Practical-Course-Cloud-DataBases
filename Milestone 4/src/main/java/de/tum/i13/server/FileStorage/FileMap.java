package de.tum.i13.server.FileStorage;


import de.tum.i13.shared.Pair;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static de.tum.i13.shared.Utility.byteToHex;

/**
 * This class is used to store the data on disk.
 * It is used to serialize and deserialize an HashMap on disk and to
 * add or remove data from the HashMap. We store the filename of the file
 * where we stored the data so that we are always able to recover it.
 * When we put a new pair in a file we must check if the number of key,value stored in that
 * file is bigger than the maxSize parameter. If is bigger then we split the
 * file in two smaller files.
 *
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class FileMap implements FileMapInterface {
    private int maxSize;
    private String fileName;
    private String hash;
    private String path;

    /**
     * Useful during the restart process to read the data from disk
     */
    public FileMap(String fileName, int MaxSize, Path path) throws FileNotFoundException {

        HashMap<String, String> hMap = read(fileName);
        if (hMap.size() > 0) {
            this.hash = getLastHash(hMap);
        } else {
            this.hash = null;
        }
        this.fileName = fileName;
        this.maxSize = MaxSize;
        this.path = path.toAbsolutePath().toString();
    }

    /**
     * Used to add a list of pair to the FileMap
     */
    public FileMap(int maxSize, ArrayList<Pair<String, String>> list, Path path) {

        HashMap<String, String> hashMap = new HashMap<>();
        for (Pair<String, String> p : list) {
            hashMap.putIfAbsent(p.getFirst(), p.getSecond());
        }
        this.hash = getLastHash(hashMap);
        this.maxSize = maxSize;
        this.path = path.toAbsolutePath().toString();
        long randomHash = String.valueOf(Math.random()).hashCode();
        fileName = path.toAbsolutePath().toString() + "/" + randomHash + ".txt";
        write(hashMap);
    }

    /**
     * Useful when we want to create a FileMap with only one element
     */
    public FileMap(int maxSize, String key, String value, Path path) {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.putIfAbsent(key, value);
        this.maxSize = maxSize;
        this.path = path.toAbsolutePath().toString();
        this.hash = getLastHash(hashMap);
        long randomHash = String.valueOf(Math.random()).hashCode();
        fileName = path.toAbsolutePath().toString() + "/" + randomHash + ".txt";
        write(hashMap);
    }

    /**
     * @return the name of the file associated with this FileMap
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * This function returns the value of the highest hash of the keys stored in this FileMap
     *
     * @return the highest hash
     */
    public String getHash() {
        return hash;
    }

    /**
     * This function is used to get the the highest hash of the keys stored in the FileMap
     *
     * @param hMap the HashMap where we want to search
     * @return the highest hash of the keys stored in the FileMap
     */
    private String getLastHash(HashMap<String, String> hMap) {
        ArrayList<Pair<String, String>> hashes = new ArrayList<>();
        hMap.forEach((key, value) -> hashes.add(new Pair<>(key, computeHash(key))));
        hashes.sort((o1, o2) -> -o1.getSecond().compareTo(o1.getFirst()));
        return hashes.get(hMap.size() - 1).getSecond();
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


    /**
     * This function is used to split the HashMap in two smaller files
     *
     * @param hMap the HashMap that we want to split
     * @return rebalanceReturn contains the new FileMap, the hash of the newFilemap and
     * the hash of the old filemap
     */
    private rebalanceReturn rebalance(HashMap<String, String> hMap) {
        // Array with the hash of each of the key that is stored in this file
        ArrayList<Pair<String, String>> hashes = new ArrayList<>();
        hMap.forEach((key, value) -> hashes.add(new Pair<String, String>(key, computeHash(key))));
        // we sort the hashes and then we split the list in two parts

        hashes.sort((o1, o2) -> -o1.getSecond().compareTo(o1.getFirst()));

        int index = hashes.size() / 2;
        String firstHash = hashes.get(index).getSecond();
        String lastHash = hashes.get(hMap.size() - 1).getSecond();

        // First part of the elements
        List<Pair<String, String>> head = hashes.subList(0, index);

        ArrayList<Pair<String, String>> newList = new ArrayList<>();
        // We add the key,value pair of the elements of the first part of the list
        // to newList
        head.forEach((p) -> {
            String key = p.getFirst();
            newList.add(new Pair<>(key, hMap.get(key)));
        });

        // we create a new filemap using the key,value pair that are in newList
        Path path = Paths.get(this.path);

        FileMap fm = new FileMap(this.maxSize, newList, path);

        // We remove from the HashMap the data that are stored in the first part of the list
        // At the end we have a new FileMap that contains the elements in the first half of the list
        // and we have a modified HashMap that contains only the elements in the second part of the list
        removeInBatch(hMap, head);
        this.hash = this.getLastHash(hMap);

        // We return the new FileMap and firstHash so that we can add a pair
        // <firstHash, fm> to the treeMap. We also return lastHash that is the hash
        // of the last element in the modified hashMap
        return new rebalanceReturn(lastHash, firstHash, fm);
    }

    /**
     * This function is used to remove a list of data from an HashMap and then
     * to serialize the HashMap on disk
     *
     * @param hMap the hashmap that we want to modify
     * @param hash the list of pairs that we want to remove from the Hashmap.
     */
    private void removeInBatch(HashMap<String, String> hMap, List<Pair<String, String>> hash) {
        for (Pair<String, String> h : hash) {
            hMap.remove(h.getFirst());
        }
        write(hMap);
    }

    /**
     * This function removes a key,value pair from a specific file.
     *
     * @param key The key that we want to remove from database on disk
     * @return the value associated with key, null if the key is not present in the database
     */
    public synchronized String remove(String key) throws FileNotFoundException {
        HashMap<String, String> hMap = read();
        String removed = hMap.remove(key);
        if (removed != null)
            write(hMap);
        return removed;
    }


    /**
     * This function is used to add a pair into the database stored on disk.
     * First of all we deserilize the Hashmap from disk.
     * When we add a new pair into the HashMap we check if the number of pairs is
     * bigger than a certain paramer, if so we call the rebalance method to
     * split the file in two smaller files.
     * At the end we serialize on disk the HashMap that contains the newly added pair.
     *
     * @param key   the key that we add to insert in our database
     * @param value the value that we add to insert in our database
     * @return null if we insert the pair without rebalance (Splitting the file in two files)
     * else return the a rebalanceReturn object where we can find the new filemap and the hashes for
     * the two filemap (the old one and the new one).
     * Returns a rebalanceReturn with null parameters if we updated tha value
     */
    public synchronized rebalanceReturn addPair(String key, String value) {

        rebalanceReturn retVal = null;
        if (this.maxSize == 1) {
            Path path = Paths.get(this.path);
            FileMap fm = new FileMap(this.maxSize, key, value, path);
            retVal = new rebalanceReturn(null, null, fm);
        } else {
            HashMap<String, String> hMap = null;
            try {
                hMap = read();
                if (hMap.put(key, value) != null) {
                    // In this case we update the value
                    write(hMap);
                    return new rebalanceReturn(null, null, null);
                }
                if (hMap.size() > this.maxSize)
                    retVal = rebalance(hMap);
                else
                    write(hMap);
            } catch (FileNotFoundException e) {

            }


        }

        return retVal;
    }

    /**
     * This function returns a key,value pair
     *
     * @param key The key whose associated value has to be returned
     * @return the value associated with key, null if the key is not present in the database
     */
    public synchronized String getValue(String key) throws FileNotFoundException {
        HashMap<String, String> hMap = read();
        return hMap.get(key);
    }

    /**
     * This function is used to serialize an HasMap to disk
     */
    private void write(HashMap<String, String> hashMap) {
        try {
            FileOutputStream fos = new FileOutputStream(this.fileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(hashMap);
            oos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * This function is used to deserialize an HasMap to disk
     */
    static HashMap<String, String> read(String fileName) throws FileNotFoundException {
        HashMap<String, String> hMap = null;
        try {
            FileInputStream fis = new FileInputStream(fileName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            hMap = (HashMap<String, String>) ois.readObject();
            ois.close();
            fis.close();
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException();
        } catch (ClassNotFoundException | IOException e) {
            System.out.println("ECCEZIONE");
            e.printStackTrace();
        }
        return hMap;
    }

    /**
     * This function is used to deserialize an HasMap from disk
     */
    private HashMap<String, String> read() throws FileNotFoundException {
        HashMap<String, String> hMap = null;
        try {
            FileInputStream fis = new FileInputStream(this.fileName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            hMap = (HashMap<String, String>) ois.readObject();
            ois.close();
            fis.close();
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return hMap;
    }
}

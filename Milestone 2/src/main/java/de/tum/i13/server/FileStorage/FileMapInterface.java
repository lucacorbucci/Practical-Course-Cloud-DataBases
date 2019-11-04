package de.tum.i13.server.FileStorage;

public interface FileMapInterface {

    /**
     * This function returns the value of the highest hash of the keys stored in this FileMap
     *
     * @return the highest hash
     */
    int getHash();

    /**
     * This function removes a key,value pair from a specific file.
     *
     * @param  key The key that we want to remove from database on disk
     * @return the value associated with key, null if the key is not present in the database
     */
    String remove(String key);

    /**
     * This function is used to add a pair into the database stored on disk.
     * First of all we deserilize the Hashmap from disk.
     * When we add a new pair into the HashMap we check if the number of pairs is
     * bigger than a certain paramer, if so we call the rebalance method to
     * split the file in two smaller files.
     * At the end we serialize on disk the HashMap that contains the newly added pair.
     *
     * @param key the key that we add to insert in our database
     * @param value the value that we add to insert in our database
     *
     * @return null if we insert the pair without rebalance (Splitting the file in two files)
     * else return the a rebalanceReturn object where we can find the new filemap and the hashes for
     * the two filemap (the old one and the new one).
     */
    public rebalanceReturn addPair(String key, String value);

    /**
     * This function returns a key,value pair
     *
     * @param  key The key that we want to remove from database on disk
     * @return the value associated with key, null if the key is not present in the database
     */
    public String getValue(String key);
}

package de.tum.i13.server.FileStorage;

import java.io.FileNotFoundException;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public interface FileStorageInterface {

    /**
     * This function is used to restore the data in the database
     * when we relaunch the server after shutdown.
     */
    void restore() throws FileNotFoundException;

    /**
     * This function adds a new pair to the database stored on disk.
     * If the key already exists in the database, the value is updated with the
     * one that we pass as parameter.
     * If the value == null the key,value pair is removed from disk
     *
     * @param key   the key that we add to insert in our database
     * @param value the value that we add to insert in our database
     * @return -1 in case of error
     * 0 if we added a new key, value pair
     * 1 if we updated the key, value
     * 2 if we deleted the key
     */
    int put(String key, String value);

    /**
     * This function returns the value associated with the parameter key
     *
     * @param key the key whose the associated value has to be returned
     * @return the value associated with key, null if key is not in the database
     */
    String get(String key);

    /**
     * This function removes the key,value pair from the database stored on disk.
     *
     * @param key the key that we want to remove
     * @return null if key is not present in the database else the value previously associated
     * with key
     */
    String remove(String key);
}

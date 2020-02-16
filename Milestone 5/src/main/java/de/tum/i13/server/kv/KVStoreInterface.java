package de.tum.i13.server.kv;

import de.tum.i13.shared.InvalidPasswordException;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public interface KVStoreInterface {

    /**
     * This function puts the <key, value> pair into the hashmap.
     *
     * @param key   the key that we want to insert
     * @param value the value that we want to insert
     * @return 0 if we added the pair to the hashmap
     * -1 in case of error
     * 1 if we update the key,value pair
     */
    int put(String key, String value, Object... password);

    /**
     * This function return the element with key=key from the hashmap.
     * If the element is not present in the hashmap returns null.
     *
     * @param key the key that we are searching for
     * @return The value of the key, null if the key is not present in the database
     */
    String get(String key, Object... pwd);

    /**
     * This function remove the <key, value> pair from the hashmap.
     *
     * @param key the key that we want to remove
     * @return 1 if we deleted the pair from the database else 0
     */
    int delete(String key, Object... pwd) throws InvalidPasswordException;

    /**
     * This function is called by the server when it is looking for the owner
     * of a specific file. It is used to send back to the client the address of the
     * the server that is responsible of that file.
     *
     * @param hash the hash of the file that we are looking for
     * @return
     */
    String getKeyRange(String hash);
}

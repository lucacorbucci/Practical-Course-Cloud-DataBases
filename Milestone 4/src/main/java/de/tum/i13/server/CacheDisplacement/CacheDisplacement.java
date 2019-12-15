package de.tum.i13.server.CacheDisplacement;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public interface CacheDisplacement {

    /**
     * This function is used when the server need to add a new pair
     * to the cache. This allow us to understand which pairs we need to
     * mantain in the cache.
     *
     * @param key the key that we want to add in the cache
     * @return The strig to be removed (when the cache is full) null otherwise
     */
    String put(String key);

    /**
     * This function is used to delete a specified key from the cache
     *
     * @param key the key that we want to remove from the cache
     * @return true if the keys was removed false otherwise
     */
    boolean remove(String key);
}

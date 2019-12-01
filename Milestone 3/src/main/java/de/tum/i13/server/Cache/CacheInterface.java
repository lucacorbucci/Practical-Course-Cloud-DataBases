package de.tum.i13.server.Cache;
/**
 *
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public interface CacheInterface {

    /**
     *  This function add the pair <key,value> to a ConcurrentHashMap.
     *  If there is enough space in the ConcurrentHashMap it simply add the pair
     *  otherwise it remove an element (based on the cache replacement policy) and
     *  then add the pair <key, value>. If there exists a pair with key = key
     *  it updates the corresponding value.
     *
     * @param   key    the key that we want to add
     * @param   value  the value that we want to add
     * @return  null in case of successfull insertion/update, key otherwise
     */
    public String put(String key, String value);

    /**
     *  This function return the element with key=key from the hashmap.
     *
     * @param   key the key that we are searching for
     * @return      The value to which the key is mapped, null if the key is not present in
     *              the concurrentHashMap
     */
    public String get(String key);

    /**
     *  This function remove the element with key=key from the concurrentHashMap.
     *
     * @param   key the key that we are searching for
     * @return      The value previously associated with the key,
     *              null if the key is not present in the concurrentHashMap
     */
    public String remove(String key);
}

package de.tum.i13.server.Cache;

import de.tum.i13.server.CacheDisplacement.CacheDisplacement;
import de.tum.i13.server.CacheDisplacement.FIFO;
import de.tum.i13.server.CacheDisplacement.LFU;
import de.tum.i13.server.CacheDisplacement.LRU;
import de.tum.i13.shared.Constants;
import de.tum.i13.shared.InvalidPasswordException;
import de.tum.i13.shared.Pair;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class Cache implements CacheInterface {
    private int maxSize;
    private ConcurrentHashMap<String, Pair<String, String>> storage;
    private String displacementStrategy;
    private int currentSize;
    private CacheDisplacement displacement;
    private Logger logger;

    /**
     * @param maxSize              maximum number of pairs that we want to store in the cache
     * @param displacementStrategy cache replacement policy
     * @throws IllegalArgumentException
     * @throws NullPointerException
     */
    public Cache(int maxSize, String displacementStrategy, Logger logger) throws IllegalArgumentException, NullPointerException {
        if (maxSize <= 0) {
            throw new IllegalArgumentException();
        }
        if (displacementStrategy == null) {
            throw new NullPointerException();
        }
        if (!(displacementStrategy.equals(Constants.LFU) || displacementStrategy.equals(Constants.LRU) || displacementStrategy.equals(Constants.FIFO))) {
            throw new IllegalArgumentException();
        }
        this.logger = logger;
        this.maxSize = maxSize;
        this.displacementStrategy = displacementStrategy;
        this.currentSize = 0;
        this.storage = new ConcurrentHashMap<>(maxSize);
        if (displacementStrategy.equals(Constants.LFU))
            displacement = new LFU(maxSize);
        else if (displacementStrategy.equals(Constants.LRU))
            displacement = new LRU(maxSize);
        else displacement = new FIFO(maxSize);
    }

    @Override
    /**
     *  This function add the pair <key,value> to a ConcurrentHashMap.
     *  If there is enough space in the ConcurrentHashMap it simply add the pair
     *  otherwise it remove an element (based on the cache replacement policy) and
     *  then add the pair <key, value>. If there exists a pair with key = key
     *  it updates the corresponding value.
     *
     * @param   key    the key that we want to add
     * @param   value  the value that we want to add
     * @return null in case of successfull insertion/update, key otherwise
     */
    public synchronized String put(String key, String value, Object... p) throws InvalidPasswordException {
        String retValue = key;

        try {
            Pair<String, String> obj = storage.get(key);
            if (obj != null && value == null) {
                if (obj.getSecond() == null) {
                    this.remove(key);
                    this.currentSize--;
                    return null;
                } else {
                    if (p.length == 0)
                        throw new InvalidPasswordException();
                    else if (obj.getSecond().equals(p[0])) {
                        this.remove(key);
                        this.currentSize--;
                        return null;
                    } else
                        throw new InvalidPasswordException();
                }
            }
        } catch (InvalidPasswordException e) {
            throw new InvalidPasswordException();
        } catch (NullPointerException | IllegalArgumentException e) {
            logger.warning("Parameter key not valid\n");
            return key;
        } catch (IllegalStateException e) {
            logger.warning("At this moment is not possible to execute this operation\n");
            return key;
        } catch (Exception e) {
            logger.warning("An error occurred\n");
            return key;
        }

        // We check if exists a pair with key in the cache. If it does not exists and the
        // cache is full we need to remove an element from the cache to add the new pair.
        if (this.currentSize == this.maxSize && !storage.containsKey(key)) {
            String toRemove = displacement.put(key);

            try {
                storage.remove(toRemove);
                this.currentSize--;
            } catch (NullPointerException | IllegalArgumentException e) {
                logger.warning("Parameter key not valid\n");
                return key;
            } catch (IllegalStateException e) {
                logger.warning("At this moment is not possible to execute this operation\n");
                return key;
            } catch (Exception e) {
                logger.warning("An error occurred\n");
                return key;
            }
        } else {
            displacement.put(key);
        }

        try {
            // We put the new pair to the cache, if the put function returns null
            // we have to increment the currentSize because we are adding a new pair
            // otherwise if it returs a string != null we do not increment currentSize
            // because we are simply updating the value
            Pair<String, String> toAdd = (p.length == 0) ? new Pair(value, null) : new Pair(value, p[0]);
            Pair<String, String> obj = storage.get(key);
            if (obj != null) {
                if (obj.getSecond() != null) {
                    if (p.length == 0)
                        throw new InvalidPasswordException();
                    else if (p[0].equals(obj.getSecond()))
                        storage.put(key, toAdd);
                    else
                        throw new InvalidPasswordException();
                } else {
                    storage.put(key, toAdd);
                }
            } else if (storage.put(key, toAdd) == null) {
                this.currentSize++;
            }
            retValue = null;
        } catch (InvalidPasswordException e) {
            throw new InvalidPasswordException();
        } catch (NullPointerException | IllegalArgumentException e) {
            logger.warning("Parameter key not valid\n");
            return key;
        } catch (IllegalStateException e) {
            logger.warning("At this moment is not possible to execute this operation\n");
            return key;
        } catch (Exception e) {
            logger.warning("An error occurred\n");
            return key;
        }

        return retValue;
    }


    private void updateCache(Pair<String, String> get, String key) {
        if (get != null) {
            if (displacementStrategy.equals(Constants.LRU)) {
                ((LRU) displacement).access(key);
            } else if (displacementStrategy.equals(Constants.LFU)) {
                ((LFU) displacement).access(key);
            }
        }
    }

    @Override
    /**
     *  This function return the element with key=key from the hashmap.
     *
     * @param   key the key that we are searching for
     * @return The value to which the key is mapped, null if the key is not present in
     *              the concurrentHashMap
     */
    public String get(String key, Object... p) throws InvalidPasswordException {
        Pair<String, String> get = null;
        try {
            get = storage.get(key);

            if (get.getSecond() == null) {
                updateCache(get, key);
                logger.info("getSecond null");
            } else {
                logger.info("length: " + p.length);
                if (p.length == 0)
                    throw new InvalidPasswordException();
                else if (get.getSecond().equals(p[0]))
                    updateCache(get, key);
                else
                    throw new InvalidPasswordException();
            }

        } catch (InvalidPasswordException e) {
            throw new InvalidPasswordException();
        } catch (NullPointerException | IllegalArgumentException e) {
            logger.warning("Parameter key not valid\n");
        } catch (IllegalStateException e) {
            logger.warning("At this moment is not possible to execute this operation\n");
        } catch (Exception e) {
            logger.warning("An error occurred\n");
        }
        return get == null ? null : get.getFirst();

    }

    @Override
    /**
     *  This function remove the element with key=key from the concurrentHashMap.
     *
     * @param   key the key that we are searching for
     * @return The value previously associated with the key,
     *              null if the key is not present in the concurrentHashMap
     */
    public synchronized String remove(String key, Object... p) throws InvalidPasswordException {
        String removed = null;
        try {
            Pair<String, String> toBeRemoved = storage.get(key);
            if (toBeRemoved != null) {
                if (toBeRemoved.getSecond() == null) {
                    removed = storage.remove(key).getFirst();
                    displacement.remove(key);
                    this.currentSize--;
                } else {
                    if (p.length == 0)
                        throw new InvalidPasswordException();
                    if (toBeRemoved.getSecond().equals(p[0])) {
                        removed = storage.remove(key).getFirst();
                        displacement.remove(key);
                        this.currentSize--;
                    } else
                        throw new InvalidPasswordException();

                }
            }

        } catch (InvalidPasswordException e) {
            throw new InvalidPasswordException();
        } catch (NullPointerException | IllegalArgumentException e) {
            logger.warning("Parameter key not valid\n");
        } catch (IllegalStateException e) {
            logger.warning("At this moment is not possible to execute this operation\n");
        } catch (Exception e) {
            logger.warning("An error occurred\n");
        }
        return removed;
    }
}

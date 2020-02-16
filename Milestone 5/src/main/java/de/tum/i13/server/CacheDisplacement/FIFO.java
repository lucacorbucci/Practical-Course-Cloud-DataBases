package de.tum.i13.server.CacheDisplacement;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class FIFO implements CacheDisplacement {

    ConcurrentLinkedQueue<String> cache;
    private int maxSize;
    private int currentSize;

    public FIFO(int size) {
        this.maxSize = size;
        this.currentSize = 0;
        cache = new ConcurrentLinkedQueue<>();
    }

    @Override
    /**
     * This function is used when the server need to add a new pair
     * to the cache. This allow us to understand which pairs we need to
     * mantain in the cache.
     * @param key the key that we want to add in the cache
     * @return The strig to be removed (when the cache is full) null otherwise
     */
    public String put(String key) {
        try {
            if (!this.cache.contains(key)) {
                String toRemove = null;
                if (currentSize == maxSize) {
                    toRemove = cache.poll();
                    this.currentSize--;
                }
                cache.offer(key);
                this.currentSize++;
                return toRemove;
            }
        } catch (NullPointerException | IllegalArgumentException e) {
            System.out.print("Parameter key not valid\n");
        }
        return null;
    }

    @Override
    /**
     * This function is used to delete a specified key from the cache
     * @param key the key that we want to remove from the cache
     * @return true if the keys was removed false otherwise
     */
    public boolean remove(String key) {
        boolean retValue = false;
        try {
            if (key == null) {
                throw new NullPointerException();
            }
            if (cache.remove(key)) {
                this.currentSize--;
                retValue = true;
            }
        } catch (NullPointerException e) {
            System.out.print("Parameter key not valid\n");
        } catch (Exception e) {
            System.out.print("An error occurred\n");
        }
        return retValue;
    }
}

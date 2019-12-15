package de.tum.i13.server.CacheDisplacement;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class LFU implements CacheDisplacement {
    private ConcurrentHashMap<String, Integer> cache;
    int maxSize;
    int currentSize;

    public LFU(int size) {
        this.maxSize = size;
        this.currentSize = 0;
        cache = new ConcurrentHashMap<>();
    }

    public int access(String key) {
        try {
            if (cache.computeIfPresent(key, (k, v) -> v += 1) == null)
                return 0;
        } catch (NullPointerException | IllegalArgumentException e) {
            System.out.print("Parameter key not valid\n");
        } catch (IllegalStateException e) {
            System.out.print("At this moment is not possible to execute this operation\n");
        } catch (RuntimeException e) {
            System.out.print("An error occurred accessing the cache\n");
        } catch (Exception e) {
            System.out.print("An error occurred\n");
        }
        return 1;
    }

    @Override
    /**
     * This function is used when the server need to add a new pair
     * to the cache. This allow us to understand which pairs we need to
     * mantain in the cache.
     * @param key the key that we want to add in the cache
     * @return The string to be removed (when the cache is full) null otherwise
     */
    public String put(String key) {
        try {
            // If the cache contains the new key I can't insert it again
            if (!cache.containsKey(key)) {
                String toRemove = null;
                if (currentSize == maxSize) {
                    var wrapper = new Object() {
                        String key = null;
                        int min = Integer.MAX_VALUE;
                    };
                    cache.forEach((k, v) -> {
                        if (v < wrapper.min) {
                            wrapper.min = v;
                            wrapper.key = k;
                        }
                    });
                    cache.remove(wrapper.key);
                    currentSize--;
                    toRemove = wrapper.key;
                }
                cache.putIfAbsent(key, 1);
                currentSize++;
                return toRemove;
            }
        } catch (NullPointerException | IllegalArgumentException e) {
            System.out.print("Parameter not valid\n");
        } catch (UnsupportedOperationException e) {
            System.out.print("Is not possible to remove the element key from cache");
        } catch (Exception e) {
            System.out.print("An error occurred\n");
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
            if (cache.remove(key) != null) {
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

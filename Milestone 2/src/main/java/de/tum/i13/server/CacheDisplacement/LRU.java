package de.tum.i13.server.CacheDisplacement;

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedDeque;
/**
 *
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class LRU  implements CacheDisplacement{
    ConcurrentLinkedDeque<String> cache;
    int maxSize;
    int currentSize;

    public LRU(int size){
        this.maxSize = size;
        this.currentSize = 0;
        this.cache = new ConcurrentLinkedDeque<>();
    }

    public int access(String key){
        try{
            if(key == null){
                throw new IllegalArgumentException();
            }
            if(cache.remove(key)){
                cache.addFirst(key);
                return 1;
            }
        } catch(NullPointerException | IllegalArgumentException e){
            System.out.print("Parameter key not valid\n");
        } catch (IllegalStateException e){
            System.out.print("At this moment is not possible to execute this operation\n");
        } catch (Exception e){
            System.out.print("An error occurred\n");
        }
        return 0;
    }

    @Override
    /**
     * This function is used when the server need to add a new pair
     * to the cache. This allow us to understand which pairs we need to
     * mantain in the cache.
     * @param key the key that we want to add in the cache
     * @return  The strig to be removed (when the cache is full) null otherwise
     */
    public String put(String key) {
        try{
            if(key == null){
                throw new IllegalArgumentException();
            }
            if(!cache.contains(key)){
                String toRemove = null;
                if(this.currentSize == this.maxSize){
                    toRemove = cache.removeLast();
                    currentSize--;
                }
                cache.addFirst(key);
                currentSize++;
                return toRemove;
            }
        } catch(NoSuchElementException e){
            System.out.print("Nothing to remove\n");
        } catch(NullPointerException | IllegalArgumentException e){
            System.out.print("Parameter key not valid\n");
        } catch (IllegalStateException e){
            System.out.print("At this moment is not possible to execute this operation\n");
        } catch (Exception e){
            System.out.print("An error occurred\n");
        }

        return null;
    }

    @Override
    /**
     * This function is used to delete a specified key from the cache
     * @param key the key that we want to remove from the cache
     * @return    true if the keys was removed false otherwise
     */
    public boolean remove(String key) {
        boolean retValue = false;
        try{
            if(cache.remove(key)){
            retValue = true;
            this.currentSize--;
        }
        } catch (NullPointerException e){
            System.out.print("Parameter key not valid\n");
        } catch (Exception e){
            System.out.print("An error occurred\n");
        }
        return retValue;
    }
}

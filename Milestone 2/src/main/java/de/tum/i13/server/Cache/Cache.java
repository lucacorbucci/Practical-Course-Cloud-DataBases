package de.tum.i13.server.Cache;

import de.tum.i13.server.CacheDisplacement.CacheDisplacement;
import de.tum.i13.server.CacheDisplacement.FIFO;
import de.tum.i13.server.CacheDisplacement.LFU;
import de.tum.i13.server.CacheDisplacement.LRU;
import de.tum.i13.shared.Constants;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 *
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class Cache implements CacheInterface{
    public static Logger logger = Logger.getLogger(Cache.class.getName());
    private int maxSize;
    private ConcurrentHashMap<String, String> storage;
    private String displacementStrategy;
    private int currentSize;
    private CacheDisplacement displacement;


    /**
     * @param maxSize maximum number of pairs that we want to store in the cache
     * @param displacementStrategy cache replacement policy
     * @throws IllegalArgumentException
     * @throws NullPointerException
     */
    public Cache(int maxSize, String displacementStrategy) throws IllegalArgumentException, NullPointerException{
        if(maxSize <= 0){
            throw new IllegalArgumentException();
        }
        if(displacementStrategy == null){
            throw new NullPointerException();
        }
        if(! (displacementStrategy.equals(Constants.LFU) || displacementStrategy.equals(Constants.LRU) || displacementStrategy.equals(Constants.FIFO))){
            throw new IllegalArgumentException();
        }
        this.maxSize = maxSize;
        this.displacementStrategy = displacementStrategy;
        this.currentSize = 0;
        this.storage = new ConcurrentHashMap<>(maxSize);
        if(displacementStrategy.equals(Constants.LFU))
            displacement = new LFU(maxSize);
        else if(displacementStrategy.equals(Constants.LRU))
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
     * @return  null in case of successfull insertion/update, key otherwise
     */
    public String put(String key, String value) {
        String retValue = key;

        try{
            if(storage.containsKey(key) && value == null){
                this.remove(key);
                this.currentSize--;
                return null;
            }
        } catch(NullPointerException | IllegalArgumentException e){
            System.out.print("Parameter key not valid\n");
            return key;
        } catch (IllegalStateException e){
            System.out.print("At this moment is not possible to execute this operation\n");
            return key;
        } catch (Exception e){
            System.out.print("An error occurred\n");
            return key;
        }


        // We check if exists a pair with key in the cache. If it does not exists and the
        // cache is full we need to remove an element from the cache to add the new pair.
        if(this.currentSize == this.maxSize && !storage.containsKey(key)){
            String toRemove = displacement.put(key);

            try{
                storage.remove(toRemove);
                this.currentSize--;
            } catch(NullPointerException | IllegalArgumentException e){
                System.out.print("Parameter key not valid\n");
                return key;
            } catch (IllegalStateException e){
                System.out.print("At this moment is not possible to execute this operation\n");
                return key;
            } catch (Exception e){
                System.out.print("An error occurred\n");
                return key;
            }
        }
        else{
            displacement.put(key);
        }
        try{
            // We put the new pair to the cache, if the put function returns null
            // we have to increment the currentSize because we are adding a new pair
            // otherwise if it returs a string != null we do not increment currentSize
            // because we are simply updating the value
            if(storage.put(key, value) == null) {
                this.currentSize++;
            }
            retValue = null;
        } catch(NullPointerException | IllegalArgumentException e){
            System.out.print("Parameter key not valid\n");
            return key;
        } catch (IllegalStateException e){
            System.out.print("At this moment is not possible to execute this operation\n");
            return key;
        } catch (Exception e){
            System.out.print("An error occurred\n");
            return key;
        }

        return retValue;
    }

    @Override
    /**
     *  This function return the element with key=key from the hashmap.
     *
     * @param   key the key that we are searching for
     * @return      The value to which the key is mapped, null if the key is not present in
     *              the concurrentHashMap
     */
    public String get(String key) {
        String get = null;
        try{
            get = storage.get(key);
            if(get != null){
                if(displacementStrategy.equals(Constants.LRU)){
                    ((LRU) displacement).access(key);
                }
                else if(displacementStrategy.equals(Constants.LFU)){
                    ((LFU) displacement).access(key);
                }
            }
        } catch(NullPointerException | IllegalArgumentException e){
            System.out.print("Parameter key not valid\n");
        } catch (IllegalStateException e){
            System.out.print("At this moment is not possible to execute this operation\n");
        } catch (Exception e){
            System.out.print("An error occurred\n");
        }
        return get;

    }

    @Override
    /**
     *  This function remove the element with key=key from the concurrentHashMap.
     *
     * @param   key the key that we are searching for
     * @return      The value previously associated with the key,
     *              null if the key is not present in the concurrentHashMap
     */
    public String remove(String key) {
        String removed = null;
        try{
            removed = storage.remove(key);
            displacement.remove(key);
            this.currentSize--;
        } catch(NullPointerException | IllegalArgumentException e){
            System.out.print("Parameter key not valid\n");
        } catch (IllegalStateException e){
            System.out.print("At this moment is not possible to execute this operation\n");
        } catch (Exception e){
            System.out.print("An error occurred\n");
        }
        return removed;
    }
}

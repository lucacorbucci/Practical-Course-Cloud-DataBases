package de.tum.i13.server.FileStorage;

/**
 * This class is used to return some useful data when we add a new pair to
 * the storage on disk and we split the file.
 * Into this rebalance Return we store the following data:
 * - A new Filemap that contain half of the data of the original FileMap
 * - The highest hash of the keys that are contained in the original Filemap
 * - The highest hash of the keys that are contained in the new filemap
 *
 * We store these two hashes because we need to store the pair <hash, Filemap>
 * in a TreeMap
 *
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class rebalanceReturn{
    private final Integer lastHash;
    private final Integer firstHash;
    private final FileMap fm;

    public rebalanceReturn(Integer lastHash, Integer firstHash, FileMap fm){
        this.lastHash = lastHash;
        this.firstHash = firstHash;
        this.fm = fm;
    }

    public FileMap getFm() {
        return fm;
    }

    public Integer getFirstHash() {
        return firstHash;
    }

    public Integer getLastHash() {
        return lastHash;
    }
}

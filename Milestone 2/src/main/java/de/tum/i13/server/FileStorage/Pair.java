package de.tum.i13.server.FileStorage;

/**
 * This class is used to store a pair. It is a generic class so the type
 * of the values that we insert in the pair can be choosen when we instantiate the class
 *
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class Pair<E, T> {
    private E key;
    private T value;

    public Pair(E key, T value){
        this.key = key;
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public E getKey() {
        return key;
    }
}

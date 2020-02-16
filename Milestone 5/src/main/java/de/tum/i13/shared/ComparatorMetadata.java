package de.tum.i13.shared;

import java.io.Serializable;
import java.util.Comparator;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class ComparatorMetadata implements Comparator, Serializable {

    /**
     * This method is used to compare two metadata
     *
     * @param o1 first metadata
     * @param o2 second metadata
     * @return a negative integer, zero, or a positive integer
     * as the specified String is greater than, equal to, or
     * less than this String, ignoring case considerations.
     */
    @Override
    public int compare(Object o1, Object o2) {
        String s1 = (String) o1;
        String s2 = (String) o2;
        return s1.compareToIgnoreCase(s2);
    }
}
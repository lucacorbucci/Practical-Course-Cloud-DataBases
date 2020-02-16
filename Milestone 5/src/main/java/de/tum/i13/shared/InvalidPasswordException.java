package de.tum.i13.shared;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class InvalidPasswordException extends Exception {
    public InvalidPasswordException() {
        super("Invalid password");
    }
}

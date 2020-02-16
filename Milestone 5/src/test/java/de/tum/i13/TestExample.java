package de.tum.i13;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
We use Junit 5 (or also called Junit Jupiter)
Many online tutorials use Junit 4, the API changed slightly
Userguide: https://junit.org/junit5/docs/current/user-guide/#writing-tests
 */
public class TestExample {

    @Test
    public void testSomething() {
        assertEquals(true, true);
    }
}

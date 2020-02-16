package de.tum.i13.CacheDisplacementTest;

import de.tum.i13.server.CacheDisplacement.LFU;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class TestLFURemove {

    private static PrintStream originalOut = System.out;
    private static PrintStream originalErr = System.err;
    private static ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private static ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private static LFU cache = new LFU(3);

    @BeforeAll
    static void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        assertNull(cache.put("Hello"));
        assertNull(cache.put("World"));
        assertNull(cache.put("Hi"));
    }

    @Test
    void testRemove() {
        assertEquals(true, cache.remove("Hello"));
        assertNull(cache.put("aaa"));
    }

    @Test
    void testRemoveNullPointerException() {
        cache.remove(null);
        assertEquals("Parameter key not valid\n", outContent.toString());
        outContent.reset();
    }

    @AfterAll
    static void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
}

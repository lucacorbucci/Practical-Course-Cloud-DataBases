package de.tum.i13.TestCache;

import de.tum.i13.server.Cache.Cache;
import de.tum.i13.server.kv.KVStore;
import de.tum.i13.shared.Constants;
import de.tum.i13.shared.InvalidPasswordException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Logger;

import static de.tum.i13.shared.LogSetup.setupLogging;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class TestAddElement {
    private static Cache cache;
    private static PrintStream originalOut = System.out;
    private static PrintStream originalErr = System.err;
    private static ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private static ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    public static Logger logger = Logger.getLogger(KVStore.class.getName());


    @BeforeAll
    static void before() throws InvalidPasswordException {
        setupLogging("OFF", logger);

        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        cache = new Cache(3, Constants.FIFO, logger);
        assertNull(cache.put("aaa", "bbb"));
        assertNull(cache.put("ddd", "bbb", "paa"));

    }

    @Test
    void putTest() throws InvalidPasswordException {
        assertNull(cache.put("Hello", "World"));
    }

    @Test
    void putTestPassword() throws InvalidPasswordException {
        assertNull(cache.put("Hello", "World", "password"));
        assertEquals("World", cache.get("Hello", "password"));

        Exception exception = assertThrows(InvalidPasswordException.class, () -> {
            cache.get("Hello", "passwprd");
        });
    }

    @Test
    void putTestContained() throws InvalidPasswordException {
        assertNull(cache.put("aaa", "dddd"));
        assertEquals("dddd", cache.get("aaa"));
    }

    @Test
    void putTestContainedPassword() throws InvalidPasswordException {
        Exception exception = assertThrows(InvalidPasswordException.class, () -> {
            cache.put("ddd", "dddd", "paaaaaa");
        });
        assertNull(cache.put("ddd", "dddd", "paa"));

        Exception exception2 = assertThrows(InvalidPasswordException.class, () -> {
            cache.get("ddd");
        });

        cache.get("ddd", "paa");
    }

    @Test
    void putTestNullPointerException() throws InvalidPasswordException {
        assertNull(cache.put(null, null));
        outContent.reset();
    }

    @Test
    void putTestNullPointerException2() throws InvalidPasswordException {
        assertNull(cache.put(null, null));
        outContent.reset();
    }

    @AfterAll
    static void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
}

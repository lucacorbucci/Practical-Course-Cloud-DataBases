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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class TestUpdate {
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
        assertEquals(null, cache.put("Hello", "World"));
        assertEquals(null, cache.put("Hello2", "World", "ciao"));
    }

    @Test
    void updateTest() throws InvalidPasswordException {
        assertEquals(null, cache.put("Hello", "AAA"));
        assertEquals("AAA", cache.get("Hello"));
    }

    @Test
    void updateTestPassword() throws InvalidPasswordException {
        Exception exception = assertThrows(InvalidPasswordException.class, () -> {
            cache.put("Hello2", "AAA");
        });
        Exception exception2 = assertThrows(InvalidPasswordException.class, () -> {
            cache.put("Hello2", "AAA", "cewew");
        });
        assertEquals(null, cache.put("Hello", "AAA", "ciao"));

        Exception exception3 = assertThrows(InvalidPasswordException.class, () -> {
            cache.get("Hello");
        });

        assertEquals("AAA", cache.get("Hello", "ciao"));
    }

    @AfterAll
    static void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
}

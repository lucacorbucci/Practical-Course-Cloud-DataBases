package de.tum.i13.TestCache;

import de.tum.i13.server.Cache.Cache;
import de.tum.i13.server.kv.KVStore;
import de.tum.i13.shared.Constants;
import de.tum.i13.shared.InvalidPasswordException;
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
public class TestRemove {
    private static Cache cache;
    private static PrintStream originalOut = System.out;
    private static PrintStream originalErr = System.err;
    private static ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private static ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    public static Logger logger = Logger.getLogger(KVStore.class.getName());

    @BeforeAll
    static void before() throws InvalidPasswordException {
        setupLogging("OFF", logger);

        cache = new Cache(10, Constants.FIFO, logger);
        assertNull(cache.put("Hello", "World"));
        assertNull(cache.put("World", "Hello"));


    }

    @Test
    void removeTest() throws InvalidPasswordException {
        assertEquals("World", cache.remove("Hello"));
    }

    @Test
    void removeTestPasssword() throws InvalidPasswordException {
        assertNull(cache.put("World2", "Hello", "ccc"));
        Exception exception = assertThrows(InvalidPasswordException.class, () -> {
            cache.remove("World2");
        });
        Exception exception2 = assertThrows(InvalidPasswordException.class, () -> {
            cache.remove("World2", "22222");
        });
        cache.remove("World2", "ccc");
    }


    @Test
    void testRemoveWithPut() throws InvalidPasswordException {
        assertNull(cache.put("World", null));
    }

    @Test
    void testRemoveWithPutAndPassword() throws InvalidPasswordException {
        assertNull(cache.put("World", null));
    }

    @Test
    void removeTestNull() throws InvalidPasswordException {
        assertNull(cache.put("World3", "Hello", "ccc"));
        Exception exception = assertThrows(InvalidPasswordException.class, () -> {
            cache.remove("World3");
        });
        Exception exception2 = assertThrows(InvalidPasswordException.class, () -> {
            cache.remove("World3", "ckwefjwen");
        });
        assertEquals("Hello", cache.remove("World3", "ccc"));
    }

    @Test
    void removeTestNullPointerException() throws InvalidPasswordException {
        assertNull(cache.remove(null));
    }


}

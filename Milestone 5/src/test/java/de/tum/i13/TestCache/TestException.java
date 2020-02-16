package de.tum.i13.TestCache;

import de.tum.i13.server.Cache.Cache;
import de.tum.i13.server.kv.KVStore;
import de.tum.i13.shared.Constants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static de.tum.i13.shared.LogSetup.setupLogging;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class TestException {
    public static Logger logger = Logger.getLogger(KVStore.class.getName());

    @BeforeAll
    static void before() {
        setupLogging("OFF", logger);

    }

    @Test
    void testIllegalArgumentException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Cache(-1, Constants.LRU, logger);
        });
    }

    @Test
    void testIllegalArgumentException2() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Cache(10, "Hello", logger);
        });
    }

    @Test
    void testNullPointerException() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            new Cache(10, null, logger);
        });
    }
}

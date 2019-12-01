package de.tum.i13.TestCache;

import de.tum.i13.server.Cache.Cache;
import de.tum.i13.shared.Constants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestException {
    @Test
    void testIllegalArgumentException(){
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Cache(-1, Constants.LRU);
        });
    }

    @Test
    void testIllegalArgumentException2(){
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Cache(10, "Hello");
        });
    }

    @Test
    void testNullPointerException(){
        Assertions.assertThrows(NullPointerException.class, () -> {
            new Cache(10, null);
        });
    }
}

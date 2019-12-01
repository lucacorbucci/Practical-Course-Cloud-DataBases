package de.tum.i13.TestCache;

import de.tum.i13.server.Cache.Cache;
import de.tum.i13.shared.Constants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestGet {
    private static Cache cache;
    private static PrintStream originalOut = System.out;
    private static PrintStream originalErr = System.err;
    private static ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private static ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    @BeforeAll
    static void before() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        cache = new Cache(3, Constants.FIFO);
        assertNull(cache.put("Hello", "World"));
    }

    @Test
    void getTest(){
        assertEquals("World", cache.get("Hello"));
    }

    @Test
    void getTestNull(){
        assertNull(cache.get("World"));
    }

    @Test
    void getTestNullPointerException(){
        cache.get(null);
        assertEquals("Parameter key not valid\n", outContent.toString());
        outContent.reset();
    }

    @AfterAll
    static void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
}

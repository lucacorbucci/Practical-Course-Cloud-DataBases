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

public class TestAddElement {
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
        assertNull(cache.put("aaa", "bbb"));

    }

    @Test
    void putTest(){
        assertNull(cache.put("Hello", "World"));
    }

    @Test
    void putTestContained(){
        assertEquals(null, cache.put("aaa", "dddd"));
        assertEquals("dddd", cache.get("aaa"));
    }

    @Test
    void putTestNullPointerException(){
        cache.put(null, null);
        assertEquals("Parameter key not valid\n", outContent.toString());
        outContent.reset();
    }

    @Test
    void putTestNullPointerException2(){
        cache.put(null, null);
        assertEquals("Parameter key not valid\n", outContent.toString());
        outContent.reset();
    }

    @AfterAll
    static void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
}

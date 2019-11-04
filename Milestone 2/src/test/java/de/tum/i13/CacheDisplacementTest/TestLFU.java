package de.tum.i13.CacheDisplacementTest;

import de.tum.i13.server.CacheDisplacement.FIFO;
import de.tum.i13.server.CacheDisplacement.LFU;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


public class TestLFU {

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
        assertNull(cache.put("Hey"));
        assertEquals(1, cache.access("Hello"));
        assertEquals(1, cache.access("Hey"));
    }

    @Test
    void testPut(){
        assertEquals("World", cache.put("Testtt"));
    }

    @Test
    void testPutContained(){
        assertNull(cache.put("Hello"));
    }

    @Test
    void testPutNULL(){
        assertNull(cache.put("Hello"));
    }

    @Test
    void testNullPointerException(){
        cache.put(null);
        assertEquals("Parameter not valid\n", outContent.toString());
        outContent.reset();
    }

    @Test
    void testAccessNone(){
        assertEquals(0, cache.access("aaaa"));
    }

    @Test
    void testAccess(){
        assertEquals(1, cache.access("Hello"));
    }

    @Test
    void testAccessNullPointer(){
        cache.access(null);
        assertEquals("Parameter key not valid\n", outContent.toString());
        outContent.reset();
    }


    @AfterAll
    static void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
}

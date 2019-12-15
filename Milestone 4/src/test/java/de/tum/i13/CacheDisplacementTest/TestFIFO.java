package de.tum.i13.CacheDisplacementTest;

import de.tum.i13.server.CacheDisplacement.FIFO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFIFO {

    private static PrintStream originalOut = System.out;
    private static PrintStream originalErr = System.err;
    private static ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private static ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    @BeforeAll
    static void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @Test
    void testNullPointerException(){
        FIFO fifo = new FIFO(10);
        fifo.put(null);
        assertEquals("Parameter key not valid\n", outContent.toString());
        outContent.reset();
    }

    @AfterAll
    static void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
}

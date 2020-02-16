package de.tum.i13.TestFileStorage;


import de.tum.i13.server.FileStorage.FileMap;
import de.tum.i13.server.FileStorage.rebalanceReturn;
import de.tum.i13.shared.InvalidPasswordException;
import de.tum.i13.shared.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class TestFileMap {
    private static FileMap fm;

    @BeforeAll
    static void before() throws InvalidPasswordException {
        Path path = Paths.get("data/");
        if (!Files.exists(path)) {
            File dir = new File(path.toString());
            dir.mkdir();
        }
        fm = new FileMap(10, "hello", new Pair<>("Hello", null), path);
        assertNull(fm.addPair("qqqq", new Pair<>("eeeee", null)));
        assertNull(fm.addPair("return", new Pair<>("eeeee", null)));
        assertNull(fm.addPair("remove", new Pair<>("eeeee", null)));

    }

    @Test
    void testAdd() throws InvalidPasswordException {
        assertNull(fm.addPair("aaa", new Pair<>("bbb", null)));
    }

    @Test
    void testChange() throws InvalidPasswordException {
        rebalanceReturn rr = fm.addPair("qqqq", new Pair<>("ciao", null));

        assertNull(rr.getFm());
        assertNull(rr.getFirstHash());
        assertNull(rr.getLastHash());
    }

    @Test
    void testGetValue() throws FileNotFoundException {
        assertEquals("eeeee", fm.getValue("return"));
    }

    @Test
    void remove() throws FileNotFoundException, InvalidPasswordException {
        assertEquals("eeeee", fm.remove("remove"));
    }

    @Test
    void removeNull() throws FileNotFoundException, InvalidPasswordException {
        assertNull(fm.remove("ciao"));
    }

    @Test
    void testIsPresent() throws FileNotFoundException {
        assertTrue(fm.isPresent("qqqq"));
        assertFalse(fm.isPresent("dahkuhjjk"));
    }

    @AfterAll
    static void afterAll() {
        Path path = Paths.get("data/");
        File[] files = new File(path.toAbsolutePath().toString() + "/").listFiles();
        for (File file : files) {
            file.delete();
        }

    }
}

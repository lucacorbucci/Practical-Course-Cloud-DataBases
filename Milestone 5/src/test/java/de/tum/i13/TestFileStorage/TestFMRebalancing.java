package de.tum.i13.TestFileStorage;


import de.tum.i13.server.FileStorage.FileMap;
import de.tum.i13.shared.InvalidPasswordException;
import de.tum.i13.shared.Pair;
import de.tum.i13.shared.Utility;
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
public class TestFMRebalancing {
    private static FileMap fm;

    @BeforeAll
    static void before() throws InvalidPasswordException {
        Path path = Paths.get("data/");
        if (!Files.exists(path)) {
            File dir = new File(path.toString());
            dir.mkdir();
        }
        fm = new FileMap(5, "hello", new Pair<>("world", null), path);
        assertNull(fm.addPair("qqqq", new Pair<>("eeeee", null)));
        assertNull(fm.addPair("return", new Pair<>("eeeee", null)));
        assertNull(fm.addPair("remove", new Pair<>("eeeee", null)));
        assertNull(fm.addPair("rebalance", new Pair<>("eeeee", null)));
    }

    @Test
    void add() throws FileNotFoundException, InvalidPasswordException {
        assertNotNull(fm.addPair("ciao", new Pair<>("mondo", null)));
        assertNull(fm.addPair("hey", new Pair<>("mondo", Utility.computeHash("password"))));
        assertEquals("mondo", fm.getValue("hey", Utility.computeHash("password")));
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

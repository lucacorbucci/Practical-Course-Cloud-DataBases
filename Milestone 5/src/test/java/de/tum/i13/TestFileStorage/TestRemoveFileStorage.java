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
public class TestRemoveFileStorage {
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
        assertNull(fm.addPair("remove", new Pair<>("eeeee", Utility.computeHash("password"))));
    }


    @Test
    void remove() throws FileNotFoundException, InvalidPasswordException {
        assertEquals("eeeee", fm.remove("return"));
        Exception exception = assertThrows(InvalidPasswordException.class, () -> {
            fm.remove("remove", Utility.computeHash("password2"));
        });
        assertEquals("eeeee", fm.remove("remove", Utility.computeHash("password")));
    }

    @Test
    void removeNull() throws FileNotFoundException, InvalidPasswordException {
        assertNull(fm.remove("ciao"));
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

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
public class TestFileMapPassword {
    private static FileMap fm;

    @BeforeAll
    static void before() throws InvalidPasswordException {
        Path path = Paths.get("data/");
        if (!Files.exists(path)) {
            File dir = new File(path.toString());
            dir.mkdir();
        }
        fm = new FileMap(10, "hello", new Pair<>("Hello", Utility.computeHash("Password")), path);
        assertNull(fm.addPair("qqqq", new Pair<>("eeeee", Utility.computeHash("pass"))));
        assertNull(fm.addPair("return", new Pair<>("eeeee", Utility.computeHash("pass1"))));
        assertNull(fm.addPair("remove", new Pair<>("eeeee", Utility.computeHash("pass2"))));
    }

    @Test
    void wrongPassword() throws FileNotFoundException {
        assertNull(fm.getValue("return", Utility.computeHash("pass2")));
    }

    @Test
    void testGetValue() throws FileNotFoundException {
        assertEquals("eeeee", fm.getValue("return", Utility.computeHash("pass1")));
    }

    @Test
    void addAndGetData() throws FileNotFoundException, InvalidPasswordException {
        assertEquals("eeeee", fm.getValue("qqqq", Utility.computeHash("pass")));
        assertEquals("eeeee", fm.getValue("return", Utility.computeHash("pass1")));
        assertEquals("eeeee", fm.getValue("remove", Utility.computeHash("pass2")));
        fm.addPair("hey", new Pair<>("value", null));
        fm.addPair("hey2", new Pair<>("value", Utility.computeHash("pass2")));
        fm.addPair("hey3", new Pair<>("value", Utility.computeHash("pass5")));
        fm.addPair("hey4", new Pair<>("value", Utility.computeHash("pass6")));
        assertEquals("value", fm.getValue("hey"));
        assertEquals("value", fm.getValue("hey2", Utility.computeHash("pass2")));
        assertEquals("value", fm.getValue("hey3", Utility.computeHash("pass5")));
        assertEquals("value", fm.getValue("hey4", Utility.computeHash("pass6")));

    }

    @Test
    void changeValue() throws InvalidPasswordException, FileNotFoundException {
        fm.addPair("heyyyy", new Pair<>("value", Utility.computeHash("pass2")));

        String expectedMessage = "Invalid password";
        Exception exception = assertThrows(InvalidPasswordException.class, () -> {
            fm.addPair("heyyyy", new Pair<>("value", Utility.computeHash("pass3")));
        });

        assertEquals(expectedMessage, exception.getMessage());

        Exception exception2 = assertThrows(InvalidPasswordException.class, () -> {
            fm.addPair("heyyyy", new Pair<>("value", null));
        });

        assertEquals(expectedMessage, exception2.getMessage());

        fm.addPair("heyyyy", new Pair<>("VAL", Utility.computeHash("pass2")));

        assertEquals("VAL", fm.getValue("heyyyy", Utility.computeHash("pass2")));

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

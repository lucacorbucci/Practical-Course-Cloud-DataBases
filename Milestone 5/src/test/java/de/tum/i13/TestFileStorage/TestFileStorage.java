package de.tum.i13.TestFileStorage;

import de.tum.i13.server.FileStorage.FileStorage;
import de.tum.i13.server.kv.KVStore;
import de.tum.i13.shared.InvalidPasswordException;
import de.tum.i13.shared.Utility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import static de.tum.i13.shared.LogSetup.setupLogging;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class TestFileStorage {

    private static FileStorage fs;
    public static Logger logger = Logger.getLogger(KVStore.class.getName());
    private static FileHandler fileHandler;

    @BeforeAll
    static void before() throws InvalidPasswordException {
        Path path = Paths.get("data/");
        if (!Files.exists(path)) {
            File dir = new File(path.toString());
            dir.mkdir();
        }
        fileHandler = setupLogging(new File("FileStorageLog.log").toPath(), "OFF", logger);
        fs = new FileStorage(path, logger);
        assertEquals(0, fs.put("Hello", "mondo"));
        assertEquals(0, fs.put("Hola", "mondo"));
        assertEquals(0, fs.put("Hola2", "mondo2", "ppp"));
        assertEquals(0, fs.put("Hola3", "mondo3", "ppp3"));

    }

    @Test
    void add() throws InvalidPasswordException {
        assertEquals(0, fs.put("ciao", "mondo"));
        assertEquals("mondo", fs.get("ciao"));

    }

    @Test
    void addWithPassword() throws InvalidPasswordException {
        assertEquals(0, fs.put("ciao22", "mondo", "password"));
        assertEquals("mondo", fs.get("ciao22", "password"));

        assertEquals(1, fs.put("ciao22", "mondo2", "password"));
        assertEquals("mondo2", fs.get("ciao22", "password"));

        Exception exception = assertThrows(InvalidPasswordException.class, () -> {
            fs.put("ciao22", "mondo3", Utility.computeHash("password2"));
        });

        assertEquals("mondo2", fs.get("ciao22", "password"));

        Exception exception2 = assertThrows(InvalidPasswordException.class, () -> {
            fs.put("ciao22", "mondo3");
        });

        assertEquals("mondo2", fs.get("ciao22", "password"));

    }

    @Test
    void get() {
        assertEquals("mondo", fs.get("Hello"));
    }

    @Test
    void getWithPassword() {
        assertEquals("mondo2", fs.get("Hola2", "ppp"));
    }


    @Test
    void getNull() {
        assertEquals(null, fs.get("qqqq"));
    }

    @Test
    void remove() throws InvalidPasswordException {
        assertEquals("mondo", fs.remove("Hola").getFirst());
    }

    @Test
    void removeWithPassword() throws InvalidPasswordException {
        assertEquals("mondo3", fs.remove("Hola3", "ppp3").getFirst());
        assertEquals(0, fs.put("ciao11", "mondo", "password"));
        Exception exception = assertThrows(InvalidPasswordException.class, () -> {
            fs.remove("ciao11", "password3333").getFirst();
        });
        Exception exception2 = assertThrows(InvalidPasswordException.class, () -> {
            fs.remove("ciao11").getFirst();
        });
    }

    @Test
    void removeNull() throws InvalidPasswordException {
        assertNull(fs.remove("cccccc").getFirst());
    }

    @Test
    void testIsUpdate() {
        assertTrue(fs.isUpdate("Hello"));
        assertFalse(fs.isUpdate("qqqqqqqqqq"));

    }

    @AfterAll
    static void afterAll() {
        Path path = Paths.get("data/");
        File[] files = new File(path.toAbsolutePath().toString() + "/").listFiles();
        for (File file : files) {
            file.delete();
        }
        fileHandler.close();
    }


}

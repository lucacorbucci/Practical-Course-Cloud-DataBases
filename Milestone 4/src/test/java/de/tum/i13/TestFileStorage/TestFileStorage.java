package de.tum.i13.TestFileStorage;

import de.tum.i13.server.FileStorage.FileStorage;
import de.tum.i13.server.kv.KVStore;
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

public class TestFileStorage {

    private static FileStorage fs;
    public static Logger logger = Logger.getLogger(KVStore.class.getName());
    private static FileHandler fileHandler;
    @BeforeAll
    static void before(){
        Path path = Paths.get("data/");
        if (!Files.exists(path)) {
            File dir = new File(path.toString());
            dir.mkdir();
        }
        fileHandler = setupLogging(new File("FileStorageLog.log").toPath(), "OFF", logger);
        fs = new FileStorage(path, logger);
        assertEquals(0, fs.put("Hello", "mondo"));
        assertEquals(0, fs.put("Hola", "mondo"));

    }

    @Test
    void add(){
        assertEquals(0, fs.put("ciao", "mondo"));
    }

    @Test
    void get(){
        assertEquals("mondo", fs.get("Hello"));
    }

    @Test
    void getNull(){
        assertEquals(null, fs.get("qqqq"));
    }

    @Test
    void remove(){
        assertEquals("mondo", fs.remove("Hola"));
    }

    @Test
    void removeNull(){
        assertNull(fs.remove("cccccc"));
    }

    @AfterAll
    static void afterAll(){
        Path path = Paths.get("data/");
        File[] files = new File(path.toAbsolutePath().toString() +"/").listFiles();
        for(File file: files){
            file.delete();
        }
        fileHandler.close();
    }

}

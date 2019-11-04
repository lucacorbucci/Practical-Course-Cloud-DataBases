package de.tum.i13.KVStoreTest;

import de.tum.i13.server.kv.KVStore;
import de.tum.i13.shared.Constants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestGet {
    private static KVStore kv;

    @BeforeAll
    static void before() {
        Path path = Paths.get("data/");
        if (!Files.exists(path)) {
            File dir = new File(path.toString());
            dir.mkdir();
        }
        kv = new KVStore(2, Constants.FIFO, path, true);
        assertEquals(0,kv.put("Hello", "World"));
    }

    @Test
    void testGet() throws Exception {
        assertEquals("World", kv.get("Hello"));
    }

    @AfterAll
    static void afterAll(){
        Path path = Paths.get("data/");
        File[] files = new File(path.toAbsolutePath().toString() +"/").listFiles();
        for(File file: files){
            file.delete();
        }
    }
}

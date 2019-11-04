package de.tum.i13.KVStoreTest;

import de.tum.i13.server.kv.KVStore;
import de.tum.i13.shared.Constants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.internal.matchers.Null;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestException {
    @Test
    void testIllegalArgumentException(){
        Path path = Paths.get("data/");
        if (!Files.exists(path)) {
            File dir = new File(path.toString());
            dir.mkdir();
        }
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
           new KVStore(-1, Constants.LRU, path, true);
        });
    }

    @Test
    void testIllegalArgumentException2(){
        Path path = Paths.get("data/");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new KVStore(10, "Hello", path, true);
        });
    }

    @Test
    void testNullPointerException(){
        Path path = Paths.get("data/");
        Assertions.assertThrows(NullPointerException.class, () -> {
            new KVStore(10, null, path, true);
        });
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

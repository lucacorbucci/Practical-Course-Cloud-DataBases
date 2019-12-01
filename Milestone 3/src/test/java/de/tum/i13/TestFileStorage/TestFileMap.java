package de.tum.i13.TestFileStorage;


import de.tum.i13.server.FileStorage.FileMap;
import de.tum.i13.server.FileStorage.rebalanceReturn;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestFileMap {
    private static FileMap fm;

    @BeforeAll
    static void before(){
        Path path = Paths.get("data/");
        if (!Files.exists(path)) {
            File dir = new File(path.toString());
            dir.mkdir();
        }
        fm = new FileMap(10, "hello", "world", path);
        assertNull(fm.addPair("qqqq", "eeeee"));
        assertNull(fm.addPair("return", "eeeee"));
        assertNull(fm.addPair("remove", "eeeee"));

    }

    @Test
    void testAdd(){
        assertNull(fm.addPair("aaa", "bbb"));
    }

    @Test
    void testChange(){
        rebalanceReturn rr = fm.addPair("qqqq", "ciao");

        assertNull(rr.getFm());
        assertNull(rr.getFirstHash());
        assertNull(rr.getLastHash());
    }

    @Test
    void testGetValue(){
        assertEquals("eeeee", fm.getValue("return"));
    }

    @Test
    void remove(){
        assertEquals("eeeee", fm.remove("remove"));
    }

    @Test
    void removeNull(){
        assertNull(fm.remove("ciao"));
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

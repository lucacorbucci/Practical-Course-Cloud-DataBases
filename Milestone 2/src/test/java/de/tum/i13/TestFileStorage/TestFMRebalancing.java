package de.tum.i13.TestFileStorage;


import de.tum.i13.server.FileStorage.FileMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class TestFMRebalancing {
    private static FileMap fm;

    @BeforeAll
    static void before(){
        Path path = Paths.get("data/");
        if (!Files.exists(path)) {
            File dir = new File(path.toString());
            dir.mkdir();
        }
        fm = new FileMap(5, "hello", "world", path);
        assertNull(fm.addPair("qqqq", "eeeee"));
        assertNull(fm.addPair("return", "eeeee"));
        assertNull(fm.addPair("remove", "eeeee"));
        assertNull(fm.addPair("rebalance", "eeeee"));
    }

    @Test
    void add(){
        assertNotNull(fm.addPair("ciao", "mondo"));
    }


}

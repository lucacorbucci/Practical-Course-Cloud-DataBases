package de.tum.i13.KVCPTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.tum.i13.server.kv.KVCommandProcessor;
import de.tum.i13.server.kv.KVStore;
import de.tum.i13.shared.Constants;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestGetKVCP {

    private static KVCommandProcessor kvcp;
    private static KVStore kvs;
    private static Path path = Paths.get("data/");

    @BeforeAll
    static void before() {

        if (!Files.exists(path)) {
            File dir = new File(path.toString());
            dir.mkdir();
        }
        kvs = new KVStore(2, Constants.FIFO, path, true);
        kvcp = new KVCommandProcessor(kvs);
        assertEquals("put_success Hola", kvcp.process("PUT Hola World\r\n"));
    }

    @Test
    void testGet1(){
        assertEquals("get_success Hola World", kvcp.process("GET Hola\r\n"));
    }

    @Test
    void testGet2(){
        assertEquals("get_error wrong amount of parameters.", kvcp.process("GET\r\n"));
    }

    @Test
    void testGet3(){
        assertEquals("get_error AAAAA key not found.", kvcp.process("GET AAAAA\r\n"));
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

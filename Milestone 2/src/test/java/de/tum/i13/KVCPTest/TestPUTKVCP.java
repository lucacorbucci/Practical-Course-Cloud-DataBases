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

public class TestPUTKVCP {

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
	void testPut1(){
		assertEquals("put_update Hola", kvcp.process("PUT Hola 111111\r\n"));
	}

	@Test
	void testPut2(){
		assertEquals("put_success Hello", kvcp.process("PUT Hello World\r\n"));
	}

	@Test
	void testPut3(){
		assertEquals("put_error wrong number of parameters", kvcp.process("PUT Hello\r\n"));
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

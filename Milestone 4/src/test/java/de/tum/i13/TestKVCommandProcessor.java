package de.tum.i13;

import de.tum.i13.server.kv.KVCommandProcessor;
import de.tum.i13.server.kv.KVStore;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TestKVCommandProcessor {

    @Test
    public void correctParsingOfPut() throws Exception {

        KVStore kv = mock(KVStore.class);
        KVCommandProcessor kvcp = new KVCommandProcessor(kv);
        kvcp.process("put key hello");

        verify(kv).put("key", "hello");
    }
}

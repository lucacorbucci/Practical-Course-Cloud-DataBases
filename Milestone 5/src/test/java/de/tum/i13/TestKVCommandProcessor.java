package de.tum.i13;

import de.tum.i13.server.kv.KVCommandProcessor;
import de.tum.i13.server.kv.KVStore;
import org.junit.jupiter.api.Test;

import java.net.SocketAddress;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TestKVCommandProcessor {

    @Test
    public void correctParsingOfPut() throws Exception {

        KVStore kv = mock(KVStore.class);
        KVCommandProcessor kvcp = new KVCommandProcessor(kv);
        SocketAddress remoteAddress = null;
        kvcp.process("put key hello", remoteAddress);

        verify(kv).put("key", "hello");
    }
}

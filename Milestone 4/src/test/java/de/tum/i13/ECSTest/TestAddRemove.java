package de.tum.i13.ECSTest;

import com.sun.source.tree.Tree;
import de.tum.i13.ECS.ECS;
import de.tum.i13.client.ActiveConnection;
import de.tum.i13.client.KVStoreLibrary;
import de.tum.i13.server.kv.KVCommandProcessor;
import de.tum.i13.server.kv.KVStore;
import de.tum.i13.server.nio.NioServer;
import de.tum.i13.shared.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.TreeMap;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class TestAddRemove {
    public static Logger logger = Logger.getLogger(TestAddRemove.class.getName());
    private static ECS ex;

    @BeforeAll
    static void beforeAll(){
        ex = new ECS(55555, "OFF");
    }

    @Test
    public void addServer() throws IOException {
        String[] req1 = new String[]{"newServer", "1.1.1.1", "1", "1", String.valueOf(Utility.getFreePort())};
        String[] req2 = new String[]{"newServer", "222.222.222.222", "2222", "2", String.valueOf(Utility.getFreePort())};
        ex.addNewServer(req1, "1.1.1.1", null, null);
        ex.addNewServer(req2, "222.222.222.222", null, null);

        TreeMap<String, DataMap> tm = new TreeMap<String, DataMap>();
        tm.put(Utility.computeHash("1.1.1.1", 1), null);
        tm.put(Utility.computeHash("222.222.222.222", 2222), null);
        Metadata metadata = new Metadata(tm);

        assertEquals(metadata.popFirstKey(), ex.getMetadata().popFirstKey());
        assertEquals(metadata.popFirstKey(), ex.getMetadata().popFirstKey());
    }

    @Test
    public void removeServer() throws IOException {
        System.out.println("remove");
        String[] req1 = new String[]{"newServer", "1.1.1.1", "1", "1", String.valueOf(Utility.getFreePort())};
        String[] req2 = new String[]{"newServer", "222.222.222.222", "2222", "2", String.valueOf(Utility.getFreePort())};

        ex.addNewServer(req1, "1.1.1.1", null, null);
        ex.addNewServer(req2, "222.222.222.222", null, null);
        String[] req3 = new String[]{"shutdownServer", "1", "1", String.valueOf(Utility.getFreePort())};
        String[] req4 = new String[]{"shutdownServer", "2222", "2", String.valueOf(Utility.getFreePort())};
        ex.shutdownServer(req3, "1.1.1.1", null, null);
        ex.shutdownServer(req4, "222.222.222.222", null, null);

        assertTrue(ex.getMetadata().isEmpty());
    }

}

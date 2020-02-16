package de.tum.i13.ECSTest;

import de.tum.i13.ECS.ECS;
import de.tum.i13.shared.DataMap;
import de.tum.i13.shared.Metadata;
import de.tum.i13.shared.Utility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.TreeMap;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class TestAddRemove {
    public static Logger logger = Logger.getLogger(TestAddRemove.class.getName());
    private static ECS ex;

    @BeforeAll
    static void beforeAll() {
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

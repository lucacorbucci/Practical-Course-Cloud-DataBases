package de.tum.i13.KVCPTest;

import de.tum.i13.ECS.ECS;
import de.tum.i13.server.kv.KVCommandProcessor;
import de.tum.i13.server.kv.KVStore;
import de.tum.i13.server.nio.NioServer;
import de.tum.i13.shared.Config;
import de.tum.i13.shared.Constants;
import de.tum.i13.shared.ServerStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class TestMoveData {


    private static ArrayList<KVStore> kv = new ArrayList<>();
    private static ArrayList<Thread> th = new ArrayList<>();
    private static ArrayList<NioServer> ns = new ArrayList<>();
    private static ArrayList<KVCommandProcessor> cmdp = new ArrayList<>();
    private static ArrayList<Integer> ports = new ArrayList<>();

    private static Thread ecs;
    public static Integer port = 5916;

    private static void launchECS() {
        ECS ex = new ECS(57334, "ALL");

        ecs = new Thread(ex);
        ecs.start();
        while (!ex.isReady()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void launchServer(int i) {

        String[] args = {"-s", "FIFO", "-c", "100", "-d", "data" + i + "/", "-l", "logFile.log", "-ll", "ALL", "-b", "127.0.0.1:57334", "-p", String.valueOf(ports.get(i))};
        Config cfg = Config.parseCommandlineArgs(args);
        kv.add(new KVStore(cfg, true, new ServerStatus(Constants.INACTIVE)));
        cmdp.add(new KVCommandProcessor(kv.get(i)));
        ns.add(new NioServer(cmdp.get(i)));
        try {

            ns.get(i).bindSockets(cfg.listenaddr, cfg.port);
            Thread t = new Thread(() -> {
                try {
                    ns.get(i).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            t.start();
            th.add(t);
        } catch (IOException e) {
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @BeforeAll
    static void before() throws IOException {
        Path path = Paths.get("data/");
        File[] files = new File(path.toAbsolutePath().toString() + "/").listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        launchECS();


    }

    @Test
    void testMoveData() throws InterruptedException {
        ports.add(51536);
        launchServer(0);
        SocketAddress remoteAddress = null;
        assertEquals("put_success Hola", cmdp.get(0).process("PUT Hola World\r\n", remoteAddress));
        assertEquals("get_success Hola World", cmdp.get(0).process("GET Hola\r\n", remoteAddress));
        assertEquals("put_success 127.0.0.151536", cmdp.get(0).process("PUT 127.0.0.151536 World\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151536 World", cmdp.get(0).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("put_success 127.0.0.151537", cmdp.get(0).process("PUT 127.0.0.151537 World\r\n", remoteAddress));

        ports.add(51537);
        launchServer(1);
        assertEquals("get_success Hola World", cmdp.get(1).process("GET Hola\r\n", remoteAddress));
        assertEquals("server_not_responsible", cmdp.get(1).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("server_not_responsible", cmdp.get(0).process("GET Hola\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151537 World", cmdp.get(1).process("GET 127.0.0.151537\r\n", remoteAddress));
        assertEquals("server_not_responsible", cmdp.get(0).process("GET 127.0.0.151537\r\n", remoteAddress));

        Thread.sleep(4000);
    }


    @AfterAll
    static void afterAll() throws InterruptedException {
        for (int i = 0; i < ports.size(); i++) {
            Path path = Paths.get("data" + i + "/");
            File[] files = new File(path.toAbsolutePath().toString() + "/").listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }

        ecs.interrupt();

        for (Thread t : th) {
            t.interrupt();
        }
        for (NioServer s : ns) {
            s.close();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

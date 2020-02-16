package de.tum.i13.KVCPTest;

import de.tum.i13.ECS.ECS;
import de.tum.i13.Util;
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
public class TestUpdateAndDelete {
    private static ArrayList<KVStore> kv = new ArrayList<>();
    private static ArrayList<Thread> th = new ArrayList<>();
    private static ArrayList<NioServer> ns = new ArrayList<>();
    private static ArrayList<KVCommandProcessor> cmdp = new ArrayList<>();
    private static ArrayList<Integer> ports = new ArrayList<>();

    private static int ECSPORT;
    private static Thread ecs;

    private static void launchECS() {
        ECS ex = new ECS(ECSPORT, "OFF");
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

        String[] args = {"-s", "FIFO", "-c", "100", "-d", "data" + i + "/", "-l", "log" + i + ".log", "-ll", "ALL", "-b", "127.0.0.1:" + ECSPORT, "-p", String.valueOf(ports.get(i))};
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
            e.printStackTrace();
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void removeFile(int i) {
        Path path = Paths.get("data" + i + "/");

        File[] files = new File(path.toAbsolutePath().toString() + "/").listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    @BeforeAll
    static void before() throws IOException, InterruptedException {
        ECSPORT = Util.getFreePort();

        launchECS();
        ports.add(51536);
        launchServer(0);
        removeFile(0);
        ports.add(51537);
        launchServer(1);
        removeFile(1);
        ports.add(51538);
        launchServer(2);
        removeFile(2);
        ports.add(51539);
        launchServer(3);
        removeFile(3);

        SocketAddress remoteAddress = null;
        assertEquals("put_success 127.0.0.151536", cmdp.get(0).process("PUT 127.0.0.151536 World\r\n", remoteAddress));
        assertEquals("put_success 127.0.0.151537", cmdp.get(1).process("PUT 127.0.0.151537 World\r\n", remoteAddress));
        assertEquals("put_success 127.0.0.151538", cmdp.get(2).process("PUT 127.0.0.151538 World\r\n", remoteAddress));
        assertEquals("put_success 127.0.0.151539", cmdp.get(3).process("PUT 127.0.0.151539 World\r\n", remoteAddress));

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        assertEquals("get_success 127.0.0.151536 World", cmdp.get(0).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151536 World", cmdp.get(3).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151536 World", cmdp.get(2).process("GET 127.0.0.151536\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.151537 World", cmdp.get(1).process("GET 127.0.0.151537\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151537 World", cmdp.get(0).process("GET 127.0.0.151537\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151537 World", cmdp.get(3).process("GET 127.0.0.151537\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.151538 World", cmdp.get(2).process("GET 127.0.0.151538\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151538 World", cmdp.get(1).process("GET 127.0.0.151538\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151538 World", cmdp.get(0).process("GET 127.0.0.151538\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.151539 World", cmdp.get(3).process("GET 127.0.0.151539\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151539 World", cmdp.get(2).process("GET 127.0.0.151539\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151539 World", cmdp.get(1).process("GET 127.0.0.151539\r\n", remoteAddress));


    }

    @Test
    void testUpdateAndDelete() throws InterruptedException {

        SocketAddress remoteAddress = null;
        assertEquals("put_update 127.0.0.151538", cmdp.get(2).process("put 127.0.0.151538 Mondo\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.151538 World", cmdp.get(1).process("GET 127.0.0.151538\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151538 World", cmdp.get(0).process("GET 127.0.0.151538\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151538 Mondo", cmdp.get(2).process("GET 127.0.0.151538\r\n", remoteAddress));

        assertEquals("put_update 127.0.0.151537", cmdp.get(1).process("put 127.0.0.151537 Ciao\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.151537 Ciao", cmdp.get(1).process("GET 127.0.0.151537\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151537 World", cmdp.get(0).process("GET 127.0.0.151537\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151537 World", cmdp.get(3).process("GET 127.0.0.151537\r\n", remoteAddress));

        assertEquals("delete_success 127.0.0.151539", cmdp.get(3).process("delete 127.0.0.151539\r\n", remoteAddress));

        assertEquals("get_error 127.0.0.151539 key not found.", cmdp.get(3).process("GET 127.0.0.151539\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151539 World", cmdp.get(2).process("GET 127.0.0.151539\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151539 World", cmdp.get(1).process("GET 127.0.0.151539\r\n", remoteAddress));

        Thread.sleep(3 * Constants.EVENTUAL_HASHING_TIMEOUT);


        assertEquals("get_success 127.0.0.151538 Mondo", cmdp.get(2).process("GET 127.0.0.151538\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151538 Mondo", cmdp.get(1).process("GET 127.0.0.151538\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151538 Mondo", cmdp.get(0).process("GET 127.0.0.151538\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.151537 Ciao", cmdp.get(1).process("GET 127.0.0.151537\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151537 Ciao", cmdp.get(0).process("GET 127.0.0.151537\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151537 Ciao", cmdp.get(3).process("GET 127.0.0.151537\r\n", remoteAddress));

        assertEquals("get_error 127.0.0.151539 key not found.", cmdp.get(3).process("GET 127.0.0.151539\r\n", remoteAddress));
        assertEquals("get_error 127.0.0.151539 key not found.", cmdp.get(2).process("GET 127.0.0.151539\r\n", remoteAddress));
        assertEquals("get_error 127.0.0.151539 key not found.", cmdp.get(1).process("GET 127.0.0.151539\r\n", remoteAddress));

    }


    @AfterAll
    static void afterAll() {
        for (int i = 0; i < ports.size(); i++) {
            Path path = Paths.get("data" + i + "/");
            File[] files = new File(path.toAbsolutePath().toString() + "/").listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }

        for (Thread t : th) {
            t.interrupt();
        }
        for (NioServer s : ns) {
            try {
                s.close();
            } catch (NullPointerException ignored) {

            }
        }


        ecs.interrupt();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

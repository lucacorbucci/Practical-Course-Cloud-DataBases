package de.tum.i13.ECSTest;

import de.tum.i13.ECS.ECS;
import de.tum.i13.Util;
import de.tum.i13.client.ActiveConnection;
import de.tum.i13.client.KVStoreLibrary;
import de.tum.i13.server.kv.KVCommandProcessor;
import de.tum.i13.server.kv.KVStore;
import de.tum.i13.server.nio.NioServer;
import de.tum.i13.shared.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import static de.tum.i13.shared.LogSetup.setupLogging;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class TestReplica {
    private static Logger logger = Logger.getLogger(TestReplica.class.getName());
    private static KVStoreLibrary kvs;
    private static PrintStream originalOut = System.out;
    private static PrintStream originalErr = System.err;
    private static ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private static ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private static FileHandler fileHandler;
    public static Integer port = 4500;

    private static ArrayList<KVStore> kv = new ArrayList<>();
    private static ArrayList<Thread> th = new ArrayList<>();
    private static ArrayList<NioServer> ns = new ArrayList<>();
    private static ArrayList<KVCommandProcessor> cmdp = new ArrayList<>();
    private static ArrayList<Integer> ports = new ArrayList<>();
    private static int ECSPORT;
    private static Thread ecs;

    private static void launchECS() {
        ECS ex = new ECS(ECSPORT, "ALL");
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
        ServerStatus serverStatus = new ServerStatus(Constants.INACTIVE);
        kv.add(new KVStore(cfg, true, serverStatus));
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

        while(!serverStatus.checkEqual(Constants.ACTIVE)){
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @BeforeAll
    static void before() throws IOException {
        ECSPORT = Util.getFreePort();
        Path path = Paths.get("data/");
        File[] files = new File(path.toAbsolutePath().toString() + "/").listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        fileHandler = setupLogging(new File("logreplica.log").toPath(), "ALL", logger);
        kvs = new KVStoreLibrary(logger, new inputPassword(false, 0));

        launchECS();

    }

    @Test
    void testReplica() throws InterruptedException {

        ports.add(port);
        launchServer(0);

        ports.add(port + 1);
        launchServer(1);

        Thread.sleep(4000);

        assertEquals("ca282e7aa9876710eac15024a364506d,6437544308a3e1831fd56643b04aca7c,127.0.0.1:4500;6437544308a3e1831fd56643b04aca7d,ca282e7aa9876710eac15024a364506c,127.0.0.1:4501;\r\n", kv.get(0).getMetadata());
        assertEquals("ca282e7aa9876710eac15024a364506d,6437544308a3e1831fd56643b04aca7c,127.0.0.1:4500;6437544308a3e1831fd56643b04aca7d,ca282e7aa9876710eac15024a364506c,127.0.0.1:4501;\r\n", kv.get(1).getMetadata());
        assertEquals("ca282e7aa9876710eac15024a364506d,6437544308a3e1831fd56643b04aca7c,127.0.0.1:4500;6437544308a3e1831fd56643b04aca7d,ca282e7aa9876710eac15024a364506c,127.0.0.1:4501;\r\n", kv.get(0).getReplicaMetadata());
        assertEquals("ca282e7aa9876710eac15024a364506d,6437544308a3e1831fd56643b04aca7c,127.0.0.1:4500;6437544308a3e1831fd56643b04aca7d,ca282e7aa9876710eac15024a364506c,127.0.0.1:4501;\r\n", kv.get(1).getReplicaMetadata());


        ActiveConnection ac = connectToServer();

        for (int i = 0; i <= 4; i++) {
            kvs.putKV(ac, new String[]{"put", "127.0.0.1" + (port + i), "World"});
            assertEquals("EchoClient> put_success 127.0.0.1" + (port + i) + "\n", outContent.toString());
            outContent.reset();
        }

        kvs.putKV(ac, new String[]{"put", "Camanfangbit", "World"});
        assertEquals("EchoClient> put_success Camanfangbit\n", outContent.toString());
        outContent.reset();
        SocketAddress remoteAddress = null;
        assertEquals("get_success Camanfangbit " + Utility.encode("World"), cmdp.get(0).process("GET Camanfangbit\r\n", remoteAddress));
        logger.info("siamo qua");
        assertEquals("get_success 127.0.0.1" + port + " " + Utility.encode("World"), cmdp.get(0).process("GET 127.0.0.1" + port + "\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.1" + (port + 1) + " " + Utility.encode("World"), cmdp.get(1).process("GET 127.0.0.1" + (port + 1) + "\r\n", remoteAddress));
        assertEquals(Constants.NOTRESPONSIBLE, cmdp.get(1).process("GET 127.0.0.1" + (port + 2) + "\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.1" + (port + 2) + " " + Utility.encode("World"), cmdp.get(0).process("GET 127.0.0.1" + (port + 2) + "\r\n", remoteAddress));
        logger.info("siamo qua 1");

        ports.add(port + 2);
        launchServer(2);
        ports.add(port + 3);
        launchServer(3);
        ports.add(port + 4);
        launchServer(4);
        logger.info("siamo qua 2");

        Thread.sleep(4000);
        logger.info("siamo qua 3");

        for (int i = 0; i <= 4; i++) {

            assertEquals("fc6464c501dd3c2f1bd46258a77b6180,4c20c72cab1ef473bb1d6e081ac40988,127.0.0.1:4502;4c20c72cab1ef473bb1d6e081ac40989,4fd5d8796710cffd51e3053fdf63c4b6,127.0.0.1:4503;" +
                    "4fd5d8796710cffd51e3053fdf63c4b7,6437544308a3e1831fd56643b04aca7c,127.0.0.1:4500;6437544308a3e1831fd56643b04aca7d,ca282e7aa9876710eac15024a364506c,127.0.0.1:4501;" +
                    "ca282e7aa9876710eac15024a364506d,fc6464c501dd3c2f1bd46258a77b617f,127.0.0.1:4504;\r\n", kv.get(i).getMetadata());

            assertEquals("fc6464c501dd3c2f1bd46258a77b6180,4c20c72cab1ef473bb1d6e081ac40988,127.0.0.1:4502;fc6464c501dd3c2f1bd46258a77b6180,4c20c72cab1ef473bb1d6e081ac40988,127.0.0.1:4503;fc6464c501dd3c2f1bd46258a77b6180,4c20c72cab1ef473bb1d6e081ac40988,127.0.0.1:4500;" +
                    "4c20c72cab1ef473bb1d6e081ac40989,4fd5d8796710cffd51e3053fdf63c4b6,127.0.0.1:4503;4c20c72cab1ef473bb1d6e081ac40989,4fd5d8796710cffd51e3053fdf63c4b6,127.0.0.1:4500;4c20c72cab1ef473bb1d6e081ac40989,4fd5d8796710cffd51e3053fdf63c4b6,127.0.0.1:4501;" +
                    "4fd5d8796710cffd51e3053fdf63c4b7,6437544308a3e1831fd56643b04aca7c,127.0.0.1:4500;4fd5d8796710cffd51e3053fdf63c4b7,6437544308a3e1831fd56643b04aca7c,127.0.0.1:4501;4fd5d8796710cffd51e3053fdf63c4b7,6437544308a3e1831fd56643b04aca7c,127.0.0.1:4504;" +
                    "6437544308a3e1831fd56643b04aca7d,ca282e7aa9876710eac15024a364506c,127.0.0.1:4501;6437544308a3e1831fd56643b04aca7d,ca282e7aa9876710eac15024a364506c,127.0.0.1:4504;6437544308a3e1831fd56643b04aca7d,ca282e7aa9876710eac15024a364506c,127.0.0.1:4502;" +
                    "ca282e7aa9876710eac15024a364506d,fc6464c501dd3c2f1bd46258a77b617f,127.0.0.1:4504;ca282e7aa9876710eac15024a364506d,fc6464c501dd3c2f1bd46258a77b617f,127.0.0.1:4502;ca282e7aa9876710eac15024a364506d,fc6464c501dd3c2f1bd46258a77b617f,127.0.0.1:4503;\r\n", kv.get(i).getReplicaMetadata());

            assertEquals("get_success 127.0.0.1" + (port + i) + " " + Utility.encode("World"), cmdp.get(i).process("GET 127.0.0.1" + (port + i) + "\r\n", remoteAddress));
        }
        Thread.sleep(3000);
        logger.info("siamo qua 4");

        assertEquals("get_success Camanfangbit " + Utility.encode("World"), cmdp.get(4).process("GET Camanfangbit\r\n", remoteAddress));
        assertEquals("get_success Camanfangbit " + Utility.encode("World"), cmdp.get(2).process("GET Camanfangbit\r\n", remoteAddress));
        assertEquals("get_success Camanfangbit " + Utility.encode("World"), cmdp.get(3).process("GET Camanfangbit\r\n", remoteAddress));
        assertEquals(Constants.NOTRESPONSIBLE, cmdp.get(0).process("GET Camanfangbit\r\n", remoteAddress));
        assertEquals(Constants.NOTRESPONSIBLE, cmdp.get(1).process("GET Camanfangbit\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.1" + (port) + " " + Utility.encode("World"), cmdp.get(0).process("GET 127.0.0.1" + (port) + "\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.1" + (port) + " " + Utility.encode("World"), cmdp.get(1).process("GET 127.0.0.1" + (port) + "\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.1" + (port) + " " + Utility.encode("World"), cmdp.get(4).process("GET 127.0.0.1" + (port) + "\r\n", remoteAddress));
        assertEquals(Constants.NOTRESPONSIBLE, cmdp.get(2).process("GET 127.0.0.1" + (port) + "\r\n", remoteAddress));
        assertEquals(Constants.NOTRESPONSIBLE, cmdp.get(3).process("GET 127.0.0.1" + (port) + "\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.1" + (port + 3) + " " + Utility.encode("World"), cmdp.get(3).process("GET 127.0.0.1" + (port + 3) + "\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.1" + (port + 3) + " " + Utility.encode("World"), cmdp.get(0).process("GET 127.0.0.1" + (port + 3) + "\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.1" + (port + 3) + " " + Utility.encode("World"), cmdp.get(1).process("GET 127.0.0.1" + (port + 3) + "\r\n", remoteAddress));
        assertEquals(Constants.NOTRESPONSIBLE, cmdp.get(2).process("GET 127.0.0.1" + (port + 3) + "\r\n", remoteAddress));
        assertEquals(Constants.NOTRESPONSIBLE, cmdp.get(4).process("GET 127.0.0.1" + (port + 3) + "\r\n", remoteAddress));

        shutdownServer(4);
        Thread.sleep(4000);

        for (int i = 0; i <= 3; i++) {

            assertEquals("ca282e7aa9876710eac15024a364506d,4c20c72cab1ef473bb1d6e081ac40988,127.0.0.1:4502;4c20c72cab1ef473bb1d6e081ac40989,4fd5d8796710cffd51e3053fdf63c4b6,127.0.0.1:4503;" +
                    "4fd5d8796710cffd51e3053fdf63c4b7,6437544308a3e1831fd56643b04aca7c,127.0.0.1:4500;6437544308a3e1831fd56643b04aca7d,ca282e7aa9876710eac15024a364506c,127.0.0.1:4501;\r\n", kv.get(i).getMetadata());

            assertEquals("ca282e7aa9876710eac15024a364506d,4c20c72cab1ef473bb1d6e081ac40988,127.0.0.1:4502;ca282e7aa9876710eac15024a364506d,4c20c72cab1ef473bb1d6e081ac40988,127.0.0.1:4503;ca282e7aa9876710eac15024a364506d,4c20c72cab1ef473bb1d6e081ac40988,127.0.0.1:4500;" +
                    "4c20c72cab1ef473bb1d6e081ac40989,4fd5d8796710cffd51e3053fdf63c4b6,127.0.0.1:4503;4c20c72cab1ef473bb1d6e081ac40989,4fd5d8796710cffd51e3053fdf63c4b6,127.0.0.1:4500;4c20c72cab1ef473bb1d6e081ac40989,4fd5d8796710cffd51e3053fdf63c4b6,127.0.0.1:4501;" +
                    "4fd5d8796710cffd51e3053fdf63c4b7,6437544308a3e1831fd56643b04aca7c,127.0.0.1:4500;4fd5d8796710cffd51e3053fdf63c4b7,6437544308a3e1831fd56643b04aca7c,127.0.0.1:4501;4fd5d8796710cffd51e3053fdf63c4b7,6437544308a3e1831fd56643b04aca7c,127.0.0.1:4502;" +
                    "6437544308a3e1831fd56643b04aca7d,ca282e7aa9876710eac15024a364506c,127.0.0.1:4501;6437544308a3e1831fd56643b04aca7d,ca282e7aa9876710eac15024a364506c,127.0.0.1:4502;6437544308a3e1831fd56643b04aca7d,ca282e7aa9876710eac15024a364506c,127.0.0.1:4503;\r\n", kv.get(i).getReplicaMetadata());

            assertEquals("get_success 127.0.0.1" + (port + i) + " " + Utility.encode("World"), cmdp.get(i).process("GET 127.0.0.1" + (port + i) + "\r\n", remoteAddress));
        }

        assertEquals("get_success 127.0.0.1" + (port) + " " + Utility.encode("World"), cmdp.get(0).process("GET 127.0.0.1" + (port) + "\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.1" + (port) + " " + Utility.encode("World"), cmdp.get(1).process("GET 127.0.0.1" + (port) + "\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.1" + (port) + " " + Utility.encode("World"), cmdp.get(2).process("GET 127.0.0.1" + (port) + "\r\n", remoteAddress));
        assertEquals(Constants.NOTRESPONSIBLE, cmdp.get(3).process("GET 127.0.0.1" + (port) + "\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.1" + (port + 4) + " " + Utility.encode("World"), cmdp.get(2).process("GET 127.0.0.1" + (port + 4) + "\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.1" + (port + 4) + " " + Utility.encode("World"), cmdp.get(3).process("GET 127.0.0.1" + (port + 4 + "\r\n"), remoteAddress));
        assertEquals("get_success 127.0.0.1" + (port + 4) + " " + Utility.encode("World"), cmdp.get(0).process("GET 127.0.0.1" + (port + 4) + "\r\n", remoteAddress));
        assertEquals(Constants.NOTRESPONSIBLE, cmdp.get(1).process("GET 127.0.0.1" + (port + 4) + "\r\n", remoteAddress));

    }


    private static void shutdownServer(int i) {
        try {
            kv.get(i).close();
            ns.get(i).close();
            th.get(i).interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private ActiveConnection connectToServer() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        ActiveConnection ac = null;


        while (ac == null) {
            try {
                ac = kvs.buildConnection(new String[]{"connect", "127.0.0.1", port.toString()});
            } catch (Exception e) {
                baos.reset();
            }
        }


        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        assertNotNull(ac);
        outContent.reset();
        return ac;
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

        ecs.interrupt();
        for (Thread t : th) {
            t.interrupt();
        }
        for (NioServer s : ns) {
            s.close();
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

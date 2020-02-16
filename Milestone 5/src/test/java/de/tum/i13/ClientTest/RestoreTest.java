package de.tum.i13.ClientTest;

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import static de.tum.i13.shared.LogSetup.setupLogging;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class RestoreTest {

    private static PrintStream originalOut = System.out;
    private static PrintStream originalErr = System.err;
    public static Integer port = 5153;
    private static Logger logger = Logger.getLogger(RestoreTest.class.getName());
    private static KVStoreLibrary kvs;
    private static Thread ecs = null;
    private static ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private static ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private static NioServer sn;
    private static FileHandler fileHandler;

    public static class conn implements Runnable {

        @Override
        public void run() {

            String[] args = {"-s", "FIFO", "-c", "100", "-d", "data/", "-l", "loggg.log", "-ll", "ALL", "-b", "127.0.0.1:51354", "-p", String.valueOf(port)};
            Config cfg = Config.parseCommandlineArgs(args);

            ServerStatus serverStatus = new ServerStatus(Constants.INACTIVE);
            KVStore kvs = new KVStore(cfg, false, serverStatus);
            CommandProcessor logic = new KVCommandProcessor(kvs);

            while (serverStatus.checkEqual(Constants.INACTIVE)) {
                if (serverStatus.checkEqual(Constants.NO_CONNECTION)) {
                    System.exit(-1);
                    return;
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            sn = new NioServer(logic);

            try {
                sn.bindSockets(cfg.listenaddr, cfg.port);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                sn.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            logger.info("KV Server started");
        }
    }

    private static void launchServer(boolean test) throws IOException, InterruptedException {

        if (ecs == null) {
            ecs = new Thread(new ECS(51354, "OFF"));
            ecs.start();
        }


        outContent.reset();
    }

    @BeforeAll
    public static void beforeAll() throws IOException, InterruptedException {
        Path path = Paths.get("data/");
        fileHandler = setupLogging(new File("logd.log").toPath(), "ALL", logger);
        kvs = new KVStoreLibrary(logger, new inputPassword(false, 0));
        File[] files = new File(path.toAbsolutePath().toString() + "/").listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        launchServer(true);
        Thread t = new Thread(new conn());
        t.start();

        ActiveConnection ac = null;


        while (ac == null) {
            try {
                ac = connectToServer();
            } catch (Exception e) {
                baos.reset();
            }

        }


        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        kvs.putKV(ac, new String[]{"put", "Hello", "World"});
        assertEquals("EchoClient> put_success Hello\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get", "Hello"});
        assertEquals("EchoClient> get_success Hello World\n", outContent.toString());
        outContent.reset();

        kvs.closeConnection(ac);

        try {
            ac.close();
            t.interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void restoreTest() throws InterruptedException, IOException {
        launchServer(false);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        ActiveConnection ac = null;

        while (ac == null) {
            try {
                ac = connectToServer();
            } catch (Exception e) {
                baos.reset();
            }

        }

        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        kvs.getValue(ac, new String[]{"get", "Hello"});
        assertEquals("EchoClient> get_success Hello World\n", outContent.toString());
        outContent.reset();

        kvs.closeConnection(ac);

    }

    private static ActiveConnection connectToServer() {
        ActiveConnection ac = kvs.buildConnection(new String[]{"connect", "localhost", String.valueOf(5153)});
        return ac;
    }


    @AfterAll
    public static void afterAll() {
        ecs.interrupt();
        sn.close();
        Path path = Paths.get("data/");
        File[] files = new File(path.toAbsolutePath().toString() + "/").listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        fileHandler.close();

        System.setOut(originalOut);
        System.setErr(originalErr);
    }
}

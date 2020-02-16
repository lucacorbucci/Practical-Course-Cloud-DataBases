package de.tum.i13.ClientTest;

import de.tum.i13.ECS.ECS;
import de.tum.i13.client.ActiveConnection;
import de.tum.i13.client.KVStoreLibrary;
import de.tum.i13.server.kv.KVCommandProcessor;
import de.tum.i13.server.kv.KVStore;
import de.tum.i13.server.nio.NioServer;
import de.tum.i13.shared.Config;
import de.tum.i13.shared.Constants;
import de.tum.i13.shared.ServerStatus;
import de.tum.i13.shared.inputPassword;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class TestRetry {
    public static Integer port = 5999;
    public static Logger logger = Logger.getLogger(TestRetry.class.getName());
    private static KVStoreLibrary kvs;
    private static PrintStream originalOut = System.out;
    private static PrintStream originalErr = System.err;
    private static ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private static ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    private static KVStore kv;
    private static Thread th;
    private static NioServer ns;
    private static Thread ecs;
    private static FileHandler fileHandler;


    private static void launchServer(boolean test) throws IOException, InterruptedException {


        ECS ex = new ECS(59857, "OFF");
        ecs = new Thread(ex);
        ecs.start();
        while (!ex.isReady()) {
            Thread.sleep(1);
        }


        String[] args = {"-s", "FIFO", "-c", "100", "-d", "dataRetry/", "-l", "logRetry.log", "-ll", "OFF", "-b", "127.0.0.1:59857", "-p", String.valueOf(port)};
        Config cfg = Config.parseCommandlineArgs(args);
        ServerStatus serverStatus = new ServerStatus(Constants.INACTIVE);
        kv = new KVStore(cfg, true, serverStatus);
        KVCommandProcessor cmdp = new KVCommandProcessor(kv);
        ns = new NioServer(cmdp);
        try {

            ns.bindSockets(cfg.listenaddr, cfg.port);

            th = new Thread(() -> {
                try {
                    ns.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            th.start();
        } catch (IOException e) {
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
    public static void beforeAll() throws IOException, InterruptedException {
        Path path = Paths.get("data/");
        fileHandler = setupLogging(new File("logd.log").toPath(), "OFF", logger);
        kvs = new KVStoreLibrary(logger, new inputPassword(false, 0));
        File[] files = new File(path.toAbsolutePath().toString() + "/").listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        launchServer(true);
    }

    @Test
    // Put a key,value pair and get it from the server
    public void putRetry() {
        ActiveConnection ac = connectToServer();
        ServerStatus status = new ServerStatus(Constants.LOCKED);
        kv.setServerStatus(status);

        Thread thNew = new Thread(() -> {
            kvs.putKV(ac, new String[]{"put", "Hello", "World"});
        });
        thNew.start();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        status = new ServerStatus(Constants.ACTIVE);
        kv.setServerStatus(status);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals("EchoClient> put_success Hello\n", outContent.toString());
        outContent.reset();
        thNew.interrupt();

        kvs.getValue(ac, new String[]{"get", "Hello"});
        assertEquals("EchoClient> get_success Hello World\n", outContent.toString());
        outContent.reset();

        kvs.closeConnection(ac);
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
    public static void afterAll() {
        th.interrupt();
        ns.close();
        ecs.interrupt();
        Path path = Paths.get("data/");
        File[] files = new File(path.toAbsolutePath().toString() + "/").listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        Path path2 = Paths.get("dataRetry/");
        File[] files2 = new File(path2.toAbsolutePath().toString() + "/").listFiles();
        if (files2 != null) {
            for (File file : files2) {
                file.delete();
            }
        }

        fileHandler.close();
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
}

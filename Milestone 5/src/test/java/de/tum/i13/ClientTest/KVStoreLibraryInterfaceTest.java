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
class KVStoreLibraryInterfaceTest {

    public static Integer port = 5909;
    private static Logger logger = Logger.getLogger(KVStoreLibraryInterfaceTest.class.getName());
    private static KVStoreLibrary kvs;
    private static PrintStream originalOut = System.out;
    private static PrintStream originalErr = System.err;
    private static ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private static ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private static FileHandler fileHandler;
    private static KVStore kv;
    private static Thread th;
    private static NioServer ns;
    private static Thread ecs;

    private static void launchServer(boolean test) throws IOException, InterruptedException {


        ECS ex = new ECS(59957, "OFF");
        ecs = new Thread(ex);
        ecs.start();
        while (!ex.isReady()) {
            Thread.sleep(1);
        }


        String[] args = {"-s", "FIFO", "-c", "100", "-d", "data/", "-l", "loggg.log", "-ll", "OFF", "-b", "127.0.0.1:59957", "-p", String.valueOf(port)};
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
    public void put() {
        ActiveConnection ac = connectToServer();

        kvs.putKV(ac, new String[]{"put", "Hello", "World"});
        assertEquals("EchoClient> put_success Hello\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get", "Hello"});
        assertEquals("EchoClient> get_success Hello World\n", outContent.toString());
        outContent.reset();

        kvs.closeConnection(ac);
    }


    @Test
    public void putTooLongKey() {
        ActiveConnection ac = connectToServer();

        kvs.putKV(ac, new String[]{"put", "ewewewrwerewewewrwerewewewrwerewewewrwerewewewrwer", "World"});
        assertEquals(String.format("EchoClient> key must be less than %s bytes, value less than %s\n", Constants.KEY_MAX_LENGTH, Constants.VALUE_MAX_LENGTH), outContent.toString());
        outContent.reset();
        kvs.closeConnection(ac);
    }

    @Test
    public void putUpdate() {

        ActiveConnection ac = connectToServer();

        kvs.putKV(ac, new String[]{"put", "AAAA", "World"});
        assertEquals("EchoClient> put_success AAAA\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get", "AAAA"});
        assertEquals("EchoClient> get_success AAAA World\n", outContent.toString());
        outContent.reset();

        kvs.putKV(ac, new String[]{"put", "AAAA", "Ciao"});
        assertEquals("EchoClient> put_update AAAA\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get", "AAAA"});
        assertEquals("EchoClient> get_success AAAA Ciao\n", outContent.toString());
        outContent.reset();

        kvs.closeConnection(ac);
    }

    @Test
    public void putError() {
        ActiveConnection ac = connectToServer();
        kvs.putKV(ac, new String[]{"put"});
        assertEquals("EchoClient> Unknown command\n", outContent.toString());
        outContent.reset();

        kvs.closeConnection(ac);

        kvs.putKV(ac, new String[]{"put", "Ciao", "World"});
        assertEquals("EchoClient> Error! Not connected!\n", outContent.toString());
        outContent.reset();

    }

    @Test
    public void putSpace() {
        ActiveConnection ac = connectToServer();
        kvs.putKV(ac, new String[]{"put", "teeeeeest", "some thing"});
        assertEquals("EchoClient> put_success teeeeeest\n", outContent.toString());
        outContent.reset();
        kvs.getValue(ac, new String[]{"get", "teeeeeest"});
        assertEquals("EchoClient> get_success teeeeeest some thing\n", outContent.toString());
        outContent.reset();

        kvs.closeConnection(ac);
    }

    @Test
    public void get() {
        ActiveConnection ac = connectToServer();

        kvs.putKV(ac, new String[]{"put", "qwerty", "World"});
        assertEquals("EchoClient> put_success qwerty\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get", "qwerty"});
        assertEquals("EchoClient> get_success qwerty World\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get", "notAKey"});
        assertEquals("EchoClient> get_error notAKey key not found.\n", outContent.toString());
        outContent.reset();

        kvs.closeConnection(ac);

    }

    @Test
    public void getCarriageReturn() {
        ActiveConnection ac = connectToServer();
        kvs.putKV(ac, new String[]{"put", "carriageReturn", "some\r\nthing"});
        assertEquals("EchoClient> put_success carriageReturn\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get", "carriageReturn"});
        assertEquals("EchoClient> get_success carriageReturn some\r\nthing\n", outContent.toString());
        outContent.reset();

        kvs.closeConnection(ac);
    }


    @Test
    public void delete() {
        ActiveConnection ac = connectToServer();

        kvs.putKV(ac, new String[]{"put", "Halo", "World"});
        assertEquals("EchoClient> put_success Halo\n", outContent.toString());
        outContent.reset();

        kvs.putKV(ac, new String[]{"put", "test2", "some\r\nthing"});
        assertEquals("EchoClient> put_success test2\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get", "Halo"});
        assertEquals("EchoClient> get_success Halo World\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get", "test2"});
        assertEquals("EchoClient> get_success test2 some\r\nthing\n", outContent.toString());
        outContent.reset();

        kvs.putKV(ac, new String[]{"put", "test2"});
        assertEquals("EchoClient> delete_success test2\n", outContent.toString());
        outContent.reset();

        kvs.putKV(ac, new String[]{"delete", "Halo"});
        assertEquals("EchoClient> delete_success Halo\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get", "Halo"});
        assertEquals("EchoClient> get_error Halo key not found.\n", outContent.toString());
        outContent.reset();
        kvs.getValue(ac, new String[]{"get", "test2"});
        assertEquals("EchoClient> get_error test2 key not found.\n", outContent.toString());
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
        kv.close();
        fileHandler.close();
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
}
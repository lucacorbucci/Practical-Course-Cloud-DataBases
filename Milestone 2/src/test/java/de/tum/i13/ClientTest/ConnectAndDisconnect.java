package de.tum.i13.ClientTest;

import de.tum.i13.Util;
import de.tum.i13.client.ActiveConnection;
import de.tum.i13.client.KVStoreLibrary;
import de.tum.i13.server.kv.KVCommandProcessor;
import de.tum.i13.server.kv.KVStore;
import de.tum.i13.server.nio.NioServer;
import de.tum.i13.shared.CommandProcessor;
import de.tum.i13.shared.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class ConnectAndDisconnect {

    public static Integer port = 5153;
    private static NioServer sn;
    private static Thread th;
    public static Logger logger = Logger.getLogger(KVStoreLibraryInterfaceTest.class.getName());
    private static KVStoreLibrary kvs = new KVStoreLibrary(logger);
    private static PrintStream originalOut = System.out;
    private static PrintStream originalErr = System.err;
    private static ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private static ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    private static void launchServer(boolean test) throws IOException, InterruptedException {
        Path path = Paths.get("data/");
        if (!Files.exists(path)) {
            File dir = new File(path.toString());
            dir.mkdir();
        }
        port = Util.getFreePort();
        CommandProcessor server = new KVCommandProcessor(new KVStore(3, Constants.FIFO, path,test));
        sn = new NioServer(server);
        sn.bindSockets("127.0.0.1", port);
        th = new Thread() {
            @Override
            public void run() {
                try {
                    sn.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        th.start();
        Thread.sleep(2000);
    }

    @BeforeAll
    public static void beforAll() throws IOException, InterruptedException {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        launchServer(true);
    }

    @Test
    void connectAndDisconnect() {
        ActiveConnection ac = kvs.buildConnection(new String[]{"connect","127.0.0.1", port.toString()});
        assertNotNull(ac);
        assertEquals("EchoClient> Connection to database server established: /127.0.0.1:" + port.toString() + "\n", outContent.toString());
        outContent.reset();
        sn.close();
        ac = kvs.buildConnection(new String[]{"connect","127.0.0.1", port.toString()});
        assertNull(ac);
        outContent.reset();
    }
}

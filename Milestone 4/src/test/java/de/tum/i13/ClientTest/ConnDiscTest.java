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
import static org.junit.jupiter.api.Assertions.*;

public class ConnDiscTest {
    public static Integer port = 5995;
    private static NioServer ns;
    private static Thread th;
    private static Logger logger = Logger.getLogger(ConnDiscTest.class.getName());
    private static KVStoreLibrary kvs ;
    private static ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private static ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private static Thread ecs;
    private static KVStore kv;
    private static PrintStream originalOut = System.out;
    private static PrintStream originalErr = System.err;
    private static FileHandler fileHandler;


    private static void launchServer() throws IOException, InterruptedException {
        ECS ex = new ECS(51399, "OFF");
        ecs = new Thread(ex);
        ecs.start();
        while(!ex.isReady()){
            Thread.sleep(1);
        }

        String[] args = {"-s", "FIFO", "-c", "100", "-d", "data/", "-l", "loggg.log", "-ll", "OFF", "-b", "127.0.0.1:51399", "-p", String.valueOf(port)};
        Config cfg = Config.parseCommandlineArgs(args);
        kv = new KVStore(cfg, true, new ServerStatus(Constants.INACTIVE));
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
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @BeforeAll
    public static void beforeAll() throws IOException, InterruptedException {
        Path path = Paths.get("data/");
        fileHandler = setupLogging(new File("logd.log").toPath(), "ALL", logger);
        kvs = new KVStoreLibrary(logger);
        File[] files = new File(path.toAbsolutePath().toString() +"/").listFiles();
        if(files != null){
            for(File file: files){
                file.delete();
            }
        }
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        launchServer();
    }

    @Test
    public void connectAndDisconnect() {
        ActiveConnection ac = kvs.buildConnection(new String[]{"connect","127.0.0.1", port.toString()});
        assertNotNull(ac);
        assertEquals("EchoClient> Connection to database server established: /127.0.0.1:" + port.toString() + "\n", outContent.toString());
        outContent.reset();
        ns.close();
        ac = kvs.buildConnection(new String[]{"connect","127.0.0.1", port.toString()});
        assertNull(ac);
        outContent.reset();
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @AfterAll
    public static void afterAll(){
        ecs.interrupt();
        th.interrupt();
        ns.close();
        fileHandler.close();
    }
}

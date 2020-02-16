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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class TestPUTKVCP {


    private static KVStore kvs;
    private static Path path = Paths.get("data/");
    private static KVStore kv;
    private static Thread th;
    private static NioServer ns;
    private static KVCommandProcessor cmdp;
    private static Thread ecs;
    public static Integer port = 5136;


    private static void launchServer() {

        ECS ex = new ECS(51555, "OFF");
        ecs = new Thread(ex);
        ecs.start();
        while (!ex.isReady()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        String[] args = {"-s", "FIFO", "-c", "100", "-d", "data/", "-l", "loggg.log", "-ll", "OFF", "-b", "127.0.0.1:51555", "-p", String.valueOf(port)};
        Config cfg = Config.parseCommandlineArgs(args);
        ServerStatus serverStatus = new ServerStatus(Constants.INACTIVE);
        kv = new KVStore(cfg, true, serverStatus);
        cmdp = new KVCommandProcessor(kv);
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
    static void before() throws IOException {
        Path path = Paths.get("data/");
        File[] files = new File(path.toAbsolutePath().toString() + "/").listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        launchServer();

        SocketAddress remoteAddress = null;
        assertEquals("put_success Hola", cmdp.process("PUT Hola World\r\n", remoteAddress));
    }

    @Test
    void testPut1() {
        SocketAddress remoteAddress = null;
        assertEquals("put_update Hola", cmdp.process("PUT Hola 111111\r\n", remoteAddress));
    }

    @Test
    void testPut2() {
        SocketAddress remoteAddress = null;
        assertEquals("put_success Hello", cmdp.process("PUT Hello World\r\n", remoteAddress));
    }

    @Test
    void testPut3() {
        SocketAddress remoteAddress = null;
        assertEquals("put_error wrong number of parameters", cmdp.process("PUT Hello\r\n", remoteAddress));
    }

    @AfterAll
    static void afterAll() {
        Path path = Paths.get("data/");
        File[] files = new File(path.toAbsolutePath().toString() + "/").listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        ecs.interrupt();
        th.interrupt();
        ns.close();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

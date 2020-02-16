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
public class TestCloseServer {
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


    private static void launchServerNOTest(int i) {

        String[] args = {"-s", "FIFO", "-c", "100", "-d", "data" + 0 + "/", "-l", "log" + i + ".log", "-ll", "ALL", "-b", "127.0.0.1:" + ECSPORT, "-p", String.valueOf(ports.get(i))};
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
        launchECS();
        ports.add(51536);
        launchServer(0);
        SocketAddress remoteAddress = null;

        assertEquals("put_success Hola", cmdp.get(0).process("PUT Hola World\r\n", remoteAddress));
        assertEquals("get_success Hola World", cmdp.get(0).process("GET Hola\r\n", remoteAddress));
        assertEquals("put_success 127.0.0.151536", cmdp.get(0).process("PUT 127.0.0.151536 World\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151536 World", cmdp.get(0).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("put_success 127.0.0.151537", cmdp.get(0).process("PUT 127.0.0.151537 World\r\n", remoteAddress));
        assertEquals("put_success 127.0.0.151538", cmdp.get(0).process("PUT 127.0.0.151538 World\r\n", remoteAddress));

        ports.add(51537);
        launchServer(1);
        assertEquals("get_success Hola World", cmdp.get(1).process("GET Hola\r\n", remoteAddress));
        assertEquals("server_not_responsible", cmdp.get(1).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("server_not_responsible", cmdp.get(0).process("GET Hola\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151537 World", cmdp.get(1).process("GET 127.0.0.151537\r\n", remoteAddress));
        assertEquals("server_not_responsible", cmdp.get(0).process("GET 127.0.0.151537\r\n", remoteAddress));

        ports.add(51538);
        launchServer(2);


        assertEquals("get_success Hola World", cmdp.get(0).process("GET Hola\r\n", remoteAddress));
        assertEquals("get_success Hola World", cmdp.get(1).process("GET Hola\r\n", remoteAddress));
        assertEquals("get_success Hola World", cmdp.get(2).process("GET Hola\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.151536 World", cmdp.get(0).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151536 World", cmdp.get(1).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151536 World", cmdp.get(2).process("GET 127.0.0.151536\r\n", remoteAddress));


        assertEquals("get_success 127.0.0.151537 World", cmdp.get(0).process("GET 127.0.0.151537\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151537 World", cmdp.get(1).process("GET 127.0.0.151537\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151538 World", cmdp.get(2).process("GET 127.0.0.151538\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.151538 World", cmdp.get(0).process("GET 127.0.0.151538\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151538 World", cmdp.get(1).process("GET 127.0.0.151538\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151538 World", cmdp.get(2).process("GET 127.0.0.151538\r\n", remoteAddress));

        assertEquals("put_success 127.0.0.151540", cmdp.get(1).process("PUT 127.0.0.151540 World\r\n", remoteAddress));
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals("get_success 127.0.0.151540 World", cmdp.get(0).process("GET 127.0.0.151540\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151540 World", cmdp.get(1).process("GET 127.0.0.151540\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151540 World", cmdp.get(2).process("GET 127.0.0.151540\r\n", remoteAddress));

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals("get_success 127.0.0.151540 World", cmdp.get(2).process("GET 127.0.0.151540\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151540 World", cmdp.get(1).process("GET 127.0.0.151540\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151540 World", cmdp.get(0).process("GET 127.0.0.151540\r\n", remoteAddress));


        ports.add(51539);
        launchServer(3);
        assertEquals("put_success 127.0.0.151539", cmdp.get(3).process("PUT 127.0.0.151539 World\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151539 World", cmdp.get(3).process("GET 127.0.0.151539\r\n", remoteAddress));
        assertEquals("server_not_responsible", cmdp.get(3).process("PUT 127.0.0.151540 World\r\n", remoteAddress));

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals("get_success 127.0.0.151539 World", cmdp.get(2).process("GET 127.0.0.151539\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151539 World", cmdp.get(1).process("GET 127.0.0.151539\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.151538 World", cmdp.get(2).process("GET 127.0.0.151538\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151538 World", cmdp.get(1).process("GET 127.0.0.151538\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151538 World", cmdp.get(0).process("GET 127.0.0.151538\r\n", remoteAddress));

        ports.add(51540);
        launchServer(4);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals("get_success 127.0.0.151538 World", cmdp.get(2).process("GET 127.0.0.151538\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151538 World", cmdp.get(4).process("GET 127.0.0.151538\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151538 World", cmdp.get(1).process("GET 127.0.0.151538\r\n", remoteAddress));
        assertEquals("server_not_responsible", cmdp.get(0).process("GET 127.0.0.151538\r\n", remoteAddress));

    }

    @Test
    void testClose() {
        SocketAddress remoteAddress = null;
        assertEquals("get_success 127.0.0.151536 World", cmdp.get(0).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151536 World", cmdp.get(2).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("server_not_responsible", cmdp.get(4).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151536 World", cmdp.get(3).process("GET 127.0.0.151536\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.151539 World", cmdp.get(2).process("GET 127.0.0.151539\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151539 World", cmdp.get(4).process("GET 127.0.0.151539\r\n", remoteAddress));
        assertEquals("server_not_responsible", cmdp.get(1).process("GET 127.0.0.151539\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151539 World", cmdp.get(3).process("GET 127.0.0.151539\r\n", remoteAddress));

        kv.get(3).close();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals("server_stopped", cmdp.get(3).process("GET 127.0.0.151536\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.151536 World", cmdp.get(0).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151536 World", cmdp.get(2).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151536 World", cmdp.get(4).process("GET 127.0.0.151536\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.151539 World", cmdp.get(2).process("GET 127.0.0.151539\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151539 World", cmdp.get(4).process("GET 127.0.0.151539\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151539 World", cmdp.get(1).process("GET 127.0.0.151539\r\n", remoteAddress));

        //*******///*******///*******///*******///*******///*******///*******///*******///*******///*******///*******///*******/

        assertEquals("get_success 127.0.0.151536 World", cmdp.get(0).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151536 World", cmdp.get(2).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("server_not_responsible", cmdp.get(1).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151536 World", cmdp.get(4).process("GET 127.0.0.151536\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.151537 World", cmdp.get(0).process("GET 127.0.0.151537\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151537 World", cmdp.get(2).process("GET 127.0.0.151537\r\n", remoteAddress));
        assertEquals("server_not_responsible", cmdp.get(4).process("GET 127.0.0.151537\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151537 World", cmdp.get(1).process("GET 127.0.0.151537\r\n", remoteAddress));


        ns.get(0).close();
        kv.get(0).close();
        th.get(0).interrupt();


        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals("server_stopped", cmdp.get(0).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("server_stopped", cmdp.get(0).process("GET 127.0.0.151539\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.151536 World", cmdp.get(2).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151536 World", cmdp.get(4).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151536 World", cmdp.get(1).process("GET 127.0.0.151536\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.151539 World", cmdp.get(2).process("GET 127.0.0.151539\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151539 World", cmdp.get(4).process("GET 127.0.0.151539\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151539 World", cmdp.get(1).process("GET 127.0.0.151539\r\n", remoteAddress));


        //*******///*******///*******///*******///*******///*******///*******///*******///*******///*******///*******///*******/

        assertEquals("get_success 127.0.0.151536 World", cmdp.get(1).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151536 World", cmdp.get(2).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151536 World", cmdp.get(4).process("GET 127.0.0.151536\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.151537 World", cmdp.get(1).process("GET 127.0.0.151537\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151537 World", cmdp.get(2).process("GET 127.0.0.151537\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151537 World", cmdp.get(4).process("GET 127.0.0.151537\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.151538 World", cmdp.get(1).process("GET 127.0.0.151538\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151538 World", cmdp.get(2).process("GET 127.0.0.151538\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151538 World", cmdp.get(4).process("GET 127.0.0.151538\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.151539 World", cmdp.get(1).process("GET 127.0.0.151539\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151539 World", cmdp.get(2).process("GET 127.0.0.151539\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151539 World", cmdp.get(4).process("GET 127.0.0.151539\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.151540 World", cmdp.get(1).process("GET 127.0.0.151540\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151540 World", cmdp.get(2).process("GET 127.0.0.151540\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151540 World", cmdp.get(4).process("GET 127.0.0.151540\r\n", remoteAddress));

        kv.get(4).close();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals("server_stopped", cmdp.get(4).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("server_stopped", cmdp.get(4).process("GET 127.0.0.151539\r\n", remoteAddress));

        assertEquals("server_not_responsible", cmdp.get(1).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151536 World", cmdp.get(2).process("GET 127.0.0.151536\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.151537 World", cmdp.get(1).process("GET 127.0.0.151537\r\n", remoteAddress));
        assertEquals("server_not_responsible", cmdp.get(2).process("GET 127.0.0.151537\r\n", remoteAddress));

        assertEquals("server_not_responsible", cmdp.get(1).process("GET 127.0.0.151538\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151538 World", cmdp.get(2).process("GET 127.0.0.151538\r\n", remoteAddress));

        assertEquals("server_not_responsible", cmdp.get(1).process("GET 127.0.0.151539\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151539 World", cmdp.get(2).process("GET 127.0.0.151539\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.151540 World", cmdp.get(1).process("GET 127.0.0.151540\r\n", remoteAddress));
        assertEquals("server_not_responsible", cmdp.get(2).process("GET 127.0.0.151540\r\n", remoteAddress));

        //*******///*******///*******///*******///*******///*******///*******///*******///*******///*******///*******///*******/


        ports.add(51541);
        launchServer(5);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals("get_success 127.0.0.151536 World", cmdp.get(5).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151536 World", cmdp.get(2).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151536 World", cmdp.get(1).process("GET 127.0.0.151536\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.151537 World", cmdp.get(5).process("GET 127.0.0.151537\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151537 World", cmdp.get(2).process("GET 127.0.0.151537\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151537 World", cmdp.get(1).process("GET 127.0.0.151537\r\n", remoteAddress));

        assertEquals("get_success 127.0.0.151538 World", cmdp.get(5).process("GET 127.0.0.151538\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151538 World", cmdp.get(2).process("GET 127.0.0.151538\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151538 World", cmdp.get(1).process("GET 127.0.0.151538\r\n", remoteAddress));

        //*******///*******///*******///*******///*******///*******///*******///*******///*******///*******///*******///*******/
        assertEquals("put_success 127.0.0.151541", cmdp.get(5).process("PUT 127.0.0.151541 World\r\n", remoteAddress));

        assertEquals("delete_success 127.0.0.151536", cmdp.get(2).process("delete 127.0.0.151536\r\n", remoteAddress));

        ports.add(51536);
        launchServerNOTest(6);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals("get_error 127.0.0.151536 key not found.", cmdp.get(6).process("GET 127.0.0.151536\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151537 World", cmdp.get(6).process("GET 127.0.0.151537\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151541 World", cmdp.get(6).process("GET 127.0.0.151541\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151541 World", cmdp.get(1).process("GET 127.0.0.151541\r\n", remoteAddress));
        assertEquals("get_success 127.0.0.151541 World", cmdp.get(5).process("GET 127.0.0.151541\r\n", remoteAddress));

    }


    @AfterAll
    static void afterAll() {
        for (int i = 0; i < ports.size(); i++) {
            Path path = Paths.get("data" + i + "/");
            File[] files = new File(path.toAbsolutePath().toString() + "/").listFiles();
            if (files != null) {
                for (File file : files) {
                    try {
                        boolean res = file.delete();
                    } catch (Exception ignored) {

                    }
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

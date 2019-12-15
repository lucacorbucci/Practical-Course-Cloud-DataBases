package de.tum.i13.KVCPTest;


import de.tum.i13.ECS.ECS;
import de.tum.i13.Util;
import de.tum.i13.client.ActiveConnection;
import de.tum.i13.client.KVStoreLibrary;
import de.tum.i13.server.kv.KVCommandProcessor;
import de.tum.i13.server.kv.KVStore;
import de.tum.i13.server.nio.NioServer;
import de.tum.i13.shared.Config;
import de.tum.i13.shared.Constants;
import de.tum.i13.shared.ServerStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Logger;

import static de.tum.i13.shared.LogSetup.setupLogging;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSIGTSTP3 {


    private static ArrayList<KVStore> kv = new ArrayList<>();
    private static ArrayList<Thread> th = new ArrayList<>();
    private static ArrayList<NioServer> ns = new ArrayList<>();
    private static ArrayList<KVCommandProcessor> cmdp = new ArrayList<>();
    private static ArrayList<Integer> ports = new ArrayList<>();
    public static Logger logger = Logger.getLogger(TestSIGTSTP.class.getName());
    private static KVStoreLibrary kvs;
    private static int ECSPORT;
    private static Thread ecs;
    private static ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private static ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    private static void launchECS(){
        ECS ex = new ECS(ECSPORT, "OFF");
        ecs = new Thread(ex);
        ecs.start();
        while(!ex.isReady()){
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
        File[] files = new File(path.toAbsolutePath().toString() +"/").listFiles();
        if(files != null){
            for(File file: files){
                file.delete();
            }
        }
        launchECS();
        setupLogging("OFF", logger);

        kvs = new KVStoreLibrary(logger);
    }

    private static void launchServer(int i){

        String[] args = {"-s", "FIFO", "-c", "100", "-d", "data" + i +"/", "-l", "log" + i + ".log", "-ll", "ALL", "-b", "127.0.0.1:" + ECSPORT, "-p", String.valueOf(ports.get(i))};
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

    @Test
    void launchServerAndThenNOTGracefullyShutdown3() throws  InterruptedException {

        ports.add(51536);
        launchServer(0);

        ports.add(51537);
        launchServer(1);

        ports.add(51538);
        launchServer(2);

        ports.add(51539);
        launchServer(3);
        assertEquals("put_success 127.0.0.151539", cmdp.get(3).process("PUT 127.0.0.151539 World\r\n"));

        Thread.sleep(4000);

        assertEquals("get_success 127.0.0.151539 World", cmdp.get(3).process("GET 127.0.0.151539\r\n"));
        assertEquals("get_success 127.0.0.151539 World", cmdp.get(2).process("GET 127.0.0.151539\r\n"));
        assertEquals("get_success 127.0.0.151539 World", cmdp.get(1).process("GET 127.0.0.151539\r\n"));


        cmdp.get(3).simulateSIGKILL();


        Thread.sleep(10000);

        assertEquals("get_success 127.0.0.151539 World", cmdp.get(2).process("GET 127.0.0.151539\r\n"));
        assertEquals("get_success 127.0.0.151539 World", cmdp.get(1).process("GET 127.0.0.151539\r\n"));
        assertEquals("get_success 127.0.0.151539 World", cmdp.get(0).process("GET 127.0.0.151539\r\n"));

        Thread.sleep(2000);


        ports.add(51540);
        launchServer(4);

        Thread.sleep(2000);

        assertEquals("get_success 127.0.0.151539 World", cmdp.get(2).process("GET 127.0.0.151539\r\n"));
        assertEquals("get_success 127.0.0.151539 World", cmdp.get(4).process("GET 127.0.0.151539\r\n"));
        assertEquals("get_success 127.0.0.151539 World", cmdp.get(1).process("GET 127.0.0.151539\r\n"));
        assertEquals("server_not_responsible", cmdp.get(0).process("GET 127.0.0.151539\r\n"));

        Thread.sleep(2000);

        cmdp.get(1).simulateSIGKILL();

        Thread.sleep(10000);

        assertEquals("get_success 127.0.0.151539 World", cmdp.get(2).process("GET 127.0.0.151539\r\n"));
        assertEquals("get_success 127.0.0.151539 World", cmdp.get(4).process("GET 127.0.0.151539\r\n"));
        assertEquals("get_success 127.0.0.151539 World", cmdp.get(0).process("GET 127.0.0.151539\r\n"));


    }

    @AfterAll
    static void afterAll(){
        for(int i=0; i<ports.size();i++){
            Path path = Paths.get("data" + i + "/");
            File[] files = new File(path.toAbsolutePath().toString() +"/").listFiles();
            if(files != null){
                for(File file: files){
                    file.delete();
                }
            }
        }

        ecs.interrupt();
        for(Thread t : th){
            t.interrupt();
        }
        for(NioServer s : ns){
            s.close();
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}

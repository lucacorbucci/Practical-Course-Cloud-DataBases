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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class KeyRangeTest {

    private static ArrayList<KVStore> kv = new ArrayList<>();
    private static ArrayList<Thread> th = new ArrayList<>();
    private static ArrayList<NioServer> ns = new ArrayList<>();
    private static ArrayList<KVCommandProcessor> cmdp = new ArrayList<>();
    private static ArrayList<Integer> ports = new ArrayList<>();
    private static int ECSPORT;
    private static Thread ecs;

    private static void launchECS(){
        ECS ex = new ECS(ECSPORT, "ALL");
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

    private static void launchServer(int i){

        String[] args = {"-s", "FIFO", "-c", "100", "-d", "data" + i +"/", "-l", "log1111.log", "-ll", "ALL", "-b", "127.0.0.1:" + ECSPORT, "-p", String.valueOf(ports.get(i))};
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
        File[] files = new File(path.toAbsolutePath().toString() +"/").listFiles();
        if(files != null){
            for(File file: files){
                file.delete();
            }
        }
        launchECS();

    }


    @Test
    void testReplica() throws InterruptedException {
        ports.add(51545);
        launchServer(0);

        String firstReturn = cmdp.get(0).process("keyrange\r\n");
        int pos = firstReturn.indexOf(" ");
        String completeReturn = cmdp.get(0).process("keyrange_read\r\n");
        int pos2 = completeReturn.indexOf(" ");
        assertEquals(firstReturn.substring(pos), completeReturn.substring(pos2));

        ports.add(51546);
        launchServer(1);
        String firstReturn01 = cmdp.get(1).process("keyrange\r\n");
        int pos01 = firstReturn.indexOf(" ");
        String completeReturn01 = cmdp.get(1).process("keyrange_read\r\n");
        int pos02 = completeReturn.indexOf(" ");
        assertEquals(firstReturn01.substring(pos01), completeReturn01.substring(pos02));

        ports.add(51547);
        launchServer(2);
        String firstReturn03 = cmdp.get(2).process("keyrange\r\n");
        int pos03 = firstReturn.indexOf(" ");
        String completeReturn03 = cmdp.get(2).process("keyrange_read\r\n");
        int pos04 = completeReturn03.indexOf(" ");
        assertNotEquals(firstReturn03.substring(pos03), completeReturn03.substring(pos04));
        String[] first = firstReturn03.split(";");
        String[] second = completeReturn03.split(";");
        assertEquals(4, first.length);
        assertEquals(10, second.length);

        ports.add(51548);
        launchServer(3);
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
        for(KVStore s : kv){
            s.close();
        }


        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

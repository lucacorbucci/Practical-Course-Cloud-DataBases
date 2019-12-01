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
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestKeyrange {

    public static Integer port1 = 7000;
    public static Integer port2 = 8000;
    public static Integer port3 = 9000;
    public static Logger logger = Logger.getLogger(KVStoreLibraryInterfaceTest.class.getName());
    private static KVStoreLibrary kvs = new KVStoreLibrary(logger);
    private static ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private static ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    private static KVStore kv1;
    private static KVCommandProcessor cmdp1;
    private static KVCommandProcessor cmdp2;
    private static KVCommandProcessor cmdp3;
    private static KVStore kv2;
    private static KVStore kv3;
    private static Thread th1;
    private static Thread th2;
    private static Thread th3;
    private static NioServer ns1;
    private static NioServer ns2;
    private static NioServer ns3;
    private static Thread ecs;
    private static ServerStatus ss1 = new ServerStatus(Constants.INACTIVE);
    private static ServerStatus ss2 = new ServerStatus(Constants.INACTIVE);
    private static ServerStatus ss3 = new ServerStatus(Constants.INACTIVE);

    private static void launchServer(boolean test) throws IOException, InterruptedException {
        ECS ex = new ECS(51360, "OFF");
        ecs = new Thread(ex);
        ecs.start();
        while(!ex.isReady()){
            Thread.sleep(1);
        }

        String[] args1 = {"-s", "FIFO", "-c", "100", "-d", "data/", "-l", "loggg.log", "-ll", "OFF", "-b", "127.0.0.1:51360", "-p", String.valueOf(port1)};
        Config cfg1 = Config.parseCommandlineArgs(args1);

        kv1 = new KVStore(cfg1, true, ss1);
        cmdp1 = new KVCommandProcessor(kv1);
        ns1 = new NioServer(cmdp1);

        String[] args2 = {"-s", "FIFO", "-c", "100", "-d", "data2/", "-l", "loggg2.log", "-ll", "OFF", "-b", "127.0.0.1:51360", "-p", String.valueOf(port2)};
        Config cfg2 = Config.parseCommandlineArgs(args2);
        kv2 = new KVStore(cfg2, true, ss2);
        cmdp2 = new KVCommandProcessor(kv2);
        ns2 = new NioServer(cmdp2);

        String[] args3 = {"-s", "FIFO", "-c", "100", "-d", "data2/", "-l", "loggg2.log", "-ll", "OFF", "-b", "127.0.0.1:51360", "-p", String.valueOf(port3)};
        Config cfg3 = Config.parseCommandlineArgs(args3);
        kv3 = new KVStore(cfg3, true, ss3);
        cmdp3 = new KVCommandProcessor(kv3);
        ns3 = new NioServer(cmdp3);

        try {

            ns1.bindSockets(cfg1.listenaddr, cfg1.port);
            ns2.bindSockets(cfg2.listenaddr, cfg2.port);
            ns3.bindSockets(cfg3.listenaddr, cfg3.port);

            th1 = new Thread(() -> {
                try {
                    ns1.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            th1.start();
            while(ss1.checkEqual(Constants.INACTIVE)){
                Thread.sleep(1);
            }

            th2 = new Thread(() -> {
                try {
                    ns2.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            th2.start();
           //while(ss2.checkEqual(Constants.INACTIVE)){
             //   Thread.sleep(1);
            //}

            th3 = new Thread(() -> {
                try {
                    ns3.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            th3.start();
            //while(ss3.checkEqual(Constants.INACTIVE)){
            //    Thread.sleep(1);
           //}
        } catch (IOException e) {
            System.out.println("failed");
            launchServer(true);
        }



    }

    @BeforeAll
    public static void beforeAll() throws IOException, InterruptedException {
        Path path = Paths.get("data/");
        File[] files = new File(path.toAbsolutePath().toString() +"/").listFiles();
        if(files != null){
            for(File file: files){
                file.delete();
            }
        }
        Path path2 = Paths.get("data2/");
        File[] files2 = new File(path2.toAbsolutePath().toString() +"/").listFiles();
        if(files2 != null){
            for(File file: files2){
                file.delete();
            }
        }
        Path path3 = Paths.get("data3/");
        File[] files3 = new File(path3.toAbsolutePath().toString() +"/").listFiles();
        if(files3 != null){
            for(File file: files3){
                file.delete();
            }
        }
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        launchServer(true);
    }

    @Test
    public void keyrange(){
        ActiveConnection ac = connectToServer(port1);

        try {
            kvs.keyRange(ac);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertEquals(kv1.getMetadata(), kvs.getMetadata());

        kvs.closeConnection(ac);
    }





    private ActiveConnection connectToServer(int port){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        ActiveConnection ac = null;


        while(ac == null){
            try{
                ac = kvs.buildConnection(new String[]{"connect","127.0.0.1", String.valueOf(port)});
            } catch (Exception e){
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
        th1.interrupt();
        th2.interrupt();
        th3.interrupt();
        ecs.interrupt();
        ns1.close();
        ns2.close();
        ns3.close();
        Path path = Paths.get("data/");
        File[] files = new File(path.toAbsolutePath().toString() +"/").listFiles();
        if(files != null){
            for(File file: files){
                file.delete();
            }
        }
        Path path2 = Paths.get("data2/");
        File[] files2 = new File(path2.toAbsolutePath().toString() +"/").listFiles();

        if(files2 != null){
            for(File file: files2){
                file.delete();
            }
        }
        Path path3 = Paths.get("data2/");
        File[] files3 = new File(path2.toAbsolutePath().toString() +"/").listFiles();

        if(files3 != null){
            for(File file: files3){
                file.delete();
            }
        }
    }
}

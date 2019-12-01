package de.tum.i13.KVCPTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.tum.i13.ECS.ECS;
import de.tum.i13.Util;
import de.tum.i13.server.nio.NioServer;
import de.tum.i13.shared.Config;
import de.tum.i13.shared.ServerStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.tum.i13.server.kv.KVCommandProcessor;
import de.tum.i13.server.kv.KVStore;
import de.tum.i13.shared.Constants;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class KVWrongCommand {

    private static KVStore kv;
    private static Thread th;
    private static NioServer ns;
    private static Thread ecs;
    private static KVCommandProcessor cmdp;
    public static Integer port = 5176;

    private static void launchServer(){
        ecs = new Thread(new ECS(51354, "OFF"));
        ecs.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String[] args = {"-s", "FIFO", "-c", "100", "-d", "data/", "-l", "loggg.log", "-ll", "OFF", "-b", "127.0.0.1:51354", "-p", String.valueOf(port)};
        Config cfg = Config.parseCommandlineArgs(args);
        kv = new KVStore(cfg, true, new ServerStatus(Constants.INACTIVE));
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
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @BeforeAll
    static void before() throws IOException {
        Path path = Paths.get("data/");
        File[] files = new File(path.toAbsolutePath().toString() +"/").listFiles();
        if(files != null){
            for(File file: files){
                file.delete();
            }
        }
        launchServer();


    }

    @Test
    void testWrongCommand(){
        assertEquals("Error. Wrong command.", cmdp.process("Hola\r\n"));
    }


    @AfterAll
    static void afterAll(){
        Path path = Paths.get("data/");
        File[] files = new File(path.toAbsolutePath().toString() +"/").listFiles();
        if(files != null){
            for(File file: files){
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

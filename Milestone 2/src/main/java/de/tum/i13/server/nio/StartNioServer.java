package de.tum.i13.server.nio;


import de.tum.i13.server.kv.KVCommandProcessor;
import de.tum.i13.server.kv.KVStore;
import de.tum.i13.shared.CommandProcessor;
import de.tum.i13.shared.Config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Logger;

import static de.tum.i13.shared.Config.parseCommandlineArgs;
import static de.tum.i13.shared.LogSetup.setupLogging;

public class StartNioServer {

    public static Logger logger = Logger.getLogger(StartNioServer.class.getName());

    public static void main(String[] args) throws IOException {
        Config cfg = parseCommandlineArgs(args);  //Do not change this
        setupLogging(cfg.logfile, cfg.loglevel);
        logger.info("Config: " + cfg.toString());

        logger.info("starting server");

        //Replace with your Key Value command processor
        if(cfg.cachedisplacement == null) {
        	cfg.cachedisplacement = "FIFO";
        }
        if(cfg.cachesize <= 0) {
        	cfg.cachesize = 100;
        }
        if (!Files.exists(cfg.dataDir)) {
            try{
                File dir = new File(cfg.dataDir.toString());
                dir.mkdir();
            } catch(Exception e){
                System.out.println("An error occurred while creating the data folder");
                return;
            }

        }
        KVStore kvs = new KVStore(cfg.cachesize, cfg.cachedisplacement, cfg.dataDir, false);
        CommandProcessor logic = new KVCommandProcessor(kvs);

        NioServer sn = new NioServer(logic);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Closing NioServer");
            sn.close();
        }));

        sn.bindSockets(cfg.listenaddr, cfg.port);
        System.out.println("KV Server started");
        sn.start();
    }
}

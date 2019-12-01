package de.tum.i13.server.nio;
import de.tum.i13.server.kv.KVCommandProcessor;
import de.tum.i13.server.kv.KVStore;
import de.tum.i13.shared.CommandProcessor;
import de.tum.i13.shared.Config;
import de.tum.i13.shared.Constants;
import de.tum.i13.shared.ServerStatus;
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

        checkParameters(cfg);

        //setupLogging(cfg.logfile, cfg.loglevel);
        setupLogging("OFF");

        logger.info("Config: " + cfg.toString());

        logger.info("starting server");

        if (!Files.exists(cfg.dataDir)) {
            try{
                File dir = new File(cfg.dataDir.toString());
                dir.mkdir();
            } catch(Exception e){
                System.out.println("An error occurred while creating the data folder");
                return;
            }

        }

        ServerStatus serverStatus = new ServerStatus(Constants.INACTIVE);
        KVStore kvs = new KVStore(cfg,false, serverStatus);
        CommandProcessor logic = new KVCommandProcessor(kvs);

        while(serverStatus.checkEqual(Constants.INACTIVE)){
            if(serverStatus.checkEqual(Constants.NO_CONNECTION)){
                System.exit(-1);
                return;
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        NioServer sn = new NioServer(logic);

        checkShutDown(sn);


        sn.bindSockets(cfg.listenaddr, cfg.port);
        logger.info("KV Server started");
        sn.start();
    }

    private static void checkShutDown(NioServer sn) {
        try{
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Closing NioServer");
                sn.close();
            }));
        } catch(Exception e){
            System.out.println("Closing.....");
        }

    }


    private static void checkParameters(Config cfg) {
        if(cfg.bootstrap == null || cfg.cachesize == 0 || cfg.cachedisplacement == null){
            Config.printHelp();
            System.exit(-1);
        }
        if(cfg.usagehelp){
            Config.printHelp();
            System.exit(0);
        }
    }

}

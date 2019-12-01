package de.tum.i13.ECS;

import de.tum.i13.shared.Constants;
import de.tum.i13.shared.Pair;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static de.tum.i13.ECS.Common.computeHash;
import static de.tum.i13.shared.LogSetup.setupLogging;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class PingThread implements Runnable{
    public static Logger logger = Logger.getLogger(PingThread.class.getName());

    private Index index;

    public PingThread(Index index){
        this.index = index;
        setupLogging("OFF");
    }

    @Override
    public void run() {
        logger.info("Started Ping Thread");
        long start = 0;
        long end = 0;
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Closing Ping Thread");
            }));
        } catch(Exception e){

        }

        while(true){
            if(index.size() > 0){
                ArrayList<Pair<String, Pair<String, Integer>>> servers = index.getServersPing();
                for (Pair<String, Pair<String, Integer>> s : servers){
                    try {
                        start = System.currentTimeMillis();
                        Socket socket = new Socket();

                        socket.connect(new InetSocketAddress(s.getSecond().getFirst(), s.getSecond().getSecond()), Constants.REPLY_TIMEOUT);
                        socket.close();
                        end = System.currentTimeMillis();
                        long toSleep = (end - start < Constants.PING_TIME) ? Constants.PING_TIME - (end-start) : 0;
                        Thread.sleep(toSleep);
                    } catch (SocketTimeoutException e) {
                        CompletableFuture.runAsync(()->{
                            logger.info("REMOVING SERVER");
                            index.removeServer(computeHash(s.getSecond().getFirst(), s.getSecond().getSecond()));
                        });
                    } catch (IOException | InterruptedException e) {
                        ;
                    }
                }
            }

        }
    }
}

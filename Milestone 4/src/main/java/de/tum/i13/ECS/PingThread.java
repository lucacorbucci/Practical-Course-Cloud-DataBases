package de.tum.i13.ECS;

import de.tum.i13.shared.Metadata;
import de.tum.i13.shared.Pair;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static de.tum.i13.shared.LogSetup.setupLogging;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class PingThread implements Runnable {
    public static Logger logger = Logger.getLogger(PingThread.class.getName());
    private Index index;

    PingThread(Index index) {
        this.index = index;
        setupLogging("ALL", logger);
    }

    /**
     * Function that sends the ping to a specific server
     *
     * @param ip      ip of the server that we want to ping
     * @param port    port of the server that we want to ping
     * @param timeout timeout for the ping
     * @param hash    hash of the server that we want to ping
     * @return a pair containg true if the server is active or false if the server
     * is not active and the hash of the server
     */
    private Pair<Boolean, String> ping(String ip, int port, int timeout, String hash) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), timeout);
            return new Pair<>(true, hash);
        } catch (IOException e) {
            return new Pair<>(false, hash);
        }
    }

    /**
     * This function is used to ping a server using asynchronously using a CompletableFuture
     *
     * @param ip      ip of the server that we want to ping
     * @param port    port of the server that we want to ping
     * @param timeout timeout for the ping
     * @param hash    hash of the server that we want to ping
     * @return A completableFuture of pair
     */
    CompletableFuture<Pair<Boolean, String>> completablePing(String ip, int port, int timeout, String hash) {
        return CompletableFuture.supplyAsync(() -> this.ping(ip, port, timeout, hash));
    }

    @Override
    public void run() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Closing Ping Thread");
            }));
        } catch (Exception ignored) {

        }
        while (true) {
            if (index.size() > 0) {
                ArrayList<Pair<String, Pair<String, Integer>>> servers = index.getServersPing();
                List<CompletableFuture<Pair<Boolean, String>>> pingFuture = servers.stream()
                        .map(pair -> completablePing(pair.getSecond().getFirst(), pair.getSecond().getSecond(), 700, pair.getFirst()))
                        .collect(Collectors.toList());

                CompletableFuture<Boolean> countFuture = CompletableFuture.allOf(
                        pingFuture.toArray(new CompletableFuture[pingFuture.size()]))
                        .thenApply(v -> pingFuture.stream()
                                .map(CompletableFuture::join)
                                .collect(Collectors.toList()))
                        .thenApply(list -> {
                            list.forEach(p -> {

                                if (!p.getFirst()) {

                                    if (index.removeServer(p.getSecond()) != null) {
                                        Metadata m = index.generateMetadata();
                                        sendMetadataToAll(m);
                                    }
                                }
                            });
                            return true;
                        });
                try {
                    countFuture.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }


    /**
     * This function is used to send the data to all the servers that are in the network
     *
     * @param metadata metadata that we want to send
     */
    private void sendMetadataToAll(Metadata metadata) {

        index.getServers().forEach(p -> {
            Socket toServer = null;
            try {
                toServer = new Socket(p.getSecond().getFirst(), p.getSecond().getSecond());
                ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(toServer.getOutputStream()));
                out.flush();
                out.writeLong("METADATA_PING".length());
                out.writeUTF("METADATA_PING");
                out.writeObject(metadata);
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


}

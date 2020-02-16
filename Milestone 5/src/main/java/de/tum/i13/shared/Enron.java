package de.tum.i13.shared;

import de.tum.i13.ECS.ECS;
import de.tum.i13.server.kv.KVCommandProcessor;
import de.tum.i13.server.kv.KVStore;
import de.tum.i13.server.nio.NioServer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class Enron {
    private static ConcurrentLinkedQueue<Pair<String, String>> queue;
    private static Thread ecs;

    public static void main(String[] args) throws InterruptedException {

        int numClient = Integer.parseInt(args[0]);
        int numServer = Integer.parseInt(args[1]);
        launchECS();
        ArrayList<Thread> tServer = launchServers(numServer);
        testEnronDataSet(numClient, numServer);

        tServer.forEach(Thread::interrupt);
        System.exit(0);
    }

    private static void launchECS() {
        ECS ex = new ECS(51399, "ALL");
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

    private static ArrayList<Thread> launchServers(int numServer) {
        ArrayList<Thread> tList = new ArrayList<>();

        for (int i = 0; i < numServer; i++) {
            int port = 5000 + i;

            String[] args = {"-s", "FIFO", "-c", "100", "-d", "data" + i + "/", "-l", "log.log", "-ll", "ALL", "-b", "127.0.0.1:51399", "-p", String.valueOf(port)};
            Config cfg = Config.parseCommandlineArgs(args);
            ServerStatus serverStatus = new ServerStatus(Constants.INACTIVE);
            KVStore kv = new KVStore(cfg, true, serverStatus);
            KVCommandProcessor cmdp = new KVCommandProcessor(kv);
            NioServer ns = new NioServer(cmdp);

            try {
                ns.bindSockets(cfg.listenaddr, cfg.port);
                Thread th = new Thread(() -> {
                    try {
                        ns.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                tList.add(th);
                th.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (serverStatus.checkEqual(Constants.INACTIVE)) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return tList;
    }


    private static void testEnronDataSet(int nThread, int nServer) {
        queue = new ConcurrentLinkedQueue<>();
        ArrayList<Thread> threadArray = new ArrayList<>();
        for (int i = 0; i < nThread; i++) {
            Thread t = new Thread(new EnronThread(queue));
            threadArray.add(t);
        }
        for (int i = 0; i < nThread; i++) {
            threadArray.get(i).start();
        }

        long startingTime = System.currentTimeMillis();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(
                    "/Users/lucacorbucci/Dropbox/Università/Magistrale/Corsi Erasmus/Cloud Databases/Milestone/5/gr5/target/dataset.txt"));
            String line = reader.readLine();
            while (line != null) {
                String[] splitted = line.split(",");
                queue.offer(new Pair<String, String>(splitted[0], splitted[1]));
                // read next line
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        for (int i = 0; i < nThread; i++) {
            queue.offer(new Pair<String, String>("-111", "-1"));
        }

        while (queue.size() != 0) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.err.print("\n");
        System.err.print("Insertion: Client: " + nThread + " Server: " + nServer + ": ");
        long endTime = System.currentTimeMillis();
        System.err.print(endTime - startingTime);
        System.err.print("\n");

        long startingTime2 = System.currentTimeMillis();

        try {
            reader = new BufferedReader(new FileReader(
                    "/Users/lucacorbucci/Dropbox/Università/Magistrale/Corsi Erasmus/Cloud Databases/Milestone/5/gr5/target/dataset.txt"));
            String line = reader.readLine();
            while (line != null) {
                String[] splitted = line.split(",");
                queue.offer(new Pair<String, String>(splitted[0], splitted[1]));
                // read next line
                line = reader.readLine();

            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        for (int i = 0; i < nThread; i++) {
            queue.offer(new Pair<String, String>("-111", "-1"));
        }

        while (queue.size() != 0) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.err.print("\n");
        System.err.print("Get all keys: Client: " + nThread + " Server: " + nServer + ": ");
        long endTime2 = System.currentTimeMillis();
        System.err.print(endTime2 - startingTime2);
        System.err.print("\n");


        long startingTime3 = System.currentTimeMillis();
        int count = 0;

        try {
            reader = new BufferedReader(new FileReader(
                    "/Users/lucacorbucci/Dropbox/Università/Magistrale/Corsi Erasmus/Cloud Databases/Milestone/5/gr5/target/dataset.txt"));
            String line = reader.readLine();
            while (line != null) {
                count++;
                if (count == 50) {
                    String[] splitted = line.split(",");
                    queue.offer(new Pair<String, String>(splitted[0], splitted[1]));
                    count = 0;
                }
                // read next line
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        for (int i = 0; i < nThread; i++) {
            queue.offer(new Pair<String, String>("-111", "-1"));
        }

        while (queue.size() != 0) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.err.print("\n");
        System.err.print("Get 100 keys: Client: " + nThread + " Server: " + nServer + ": ");
        long endTime3 = System.currentTimeMillis();
        System.err.print(endTime3 - startingTime3);
        System.err.print("\n");

    }


}
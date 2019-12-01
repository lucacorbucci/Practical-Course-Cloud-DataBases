package de.tum.i13.shared;

import de.tum.i13.server.kv.KVCommandProcessor;
import de.tum.i13.server.kv.KVStore;
import de.tum.i13.server.nio.NioServer;
import de.tum.i13.shared.Config;
import de.tum.i13.shared.Constants;
import de.tum.i13.shared.Pair;
import de.tum.i13.shared.ServerStatus;

import java.io.*;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;



/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class Enron {
    private static ConcurrentLinkedQueue<Pair<String, String>> queue;

    public static void main(String[] args) throws InterruptedException {

        int numClient = Integer.parseInt(args[0]);
        int numServer = Integer.parseInt(args[1]);
        testEnronDataSet(numClient, numServer);
        /*
        ArrayList<Thread> tServer = launchServers(numServer);
        tServer.forEach(Thread::interrupt);
        System.err.println("ok");
        System.exit(0);*/
    }



    private static ArrayList<Thread> launchServers(int numServer){
        ArrayList<Thread> tList = new ArrayList<>();

        for(int i = 0; i < numServer; i++){
            int port = 5000+i;

            String[] args = {"-s", "FIFO", "-c", "100", "-d", "data" + i + "/", "-l", "log.log", "-ll", "OFF", "-b", "127.0.0.1:51354", "-p", String.valueOf(port)};
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

            while(serverStatus.checkEqual(Constants.INACTIVE)){
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return tList;
    }


    private static void testEnronDataSet(int nThread, int nServer){
        int count = 0;
  /*      queue = new ConcurrentLinkedQueue<>();
        ArrayList<Thread> threadArray = new ArrayList<>();
        for(int i = 0; i < nThread; i++){
            Thread t = new Thread(new EnronThread(queue));
            threadArray.add(t);
        }
        for(int i = 0; i < nThread; i++){
            threadArray.get(i).start();
        }
*/
        long startingTime = System.currentTimeMillis();
        //iterate over directory structure and buid key: maildir/nameX/all_documents-x value: (filevalue of x)
        File dir = new File("/Users/lucacorbucci/Documents/EnronMail/mailDB/maildir"); //path to the enron dataset
        File[] directoryList = dir.listFiles();
        if (directoryList != null){
            for(File dirElement : directoryList){
                File[] firstLevelList = dirElement.listFiles();
                if (firstLevelList != null){
                    for (File firstLevelElement : firstLevelList){
                        if (firstLevelElement.getName().equals("all_documents")){
                            File [] fileList = firstLevelElement.listFiles();
                            if (fileList != null){
                                for (File fileListElement : fileList){
                                    String value = readFile(fileListElement);//content of file
                                    //queue.offer(new Pair<String, String>(//key
                                    //        fileListElement.getName()//content of file
                                    //        , value));
                                    count++;
                                }
                            }
                        }
                    }
                }
            }
        }
        /*
        for(int i = 0; i < nThread; i++){
            queue.offer(new Pair<String, String>("-111", "-1"));
        }

        while(queue.size() != 0){
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        while(threadArray.size() != 0){
            for(int i = 0; i < threadArray.size(); i++){
                if(!threadArray.get(i).isAlive()){
                    threadArray.remove(i);
                }
            }
        }

        System.err.println("END");
        System.out.println();

        System.err.print("Client: " + nThread + " Server: " + nServer + ": ");
        long endTime = System.currentTimeMillis();
        System.err.print(endTime-startingTime);

        System.out.println();
        System.err.println("END");*/
        System.out.println(count);

    }





    public static String readFile(File f){
        StringBuilder result = new StringBuilder();
        String lineText = null;
        try {
            BufferedReader lineReader = new BufferedReader(new FileReader(f));
            while ((lineText = lineReader.readLine()) != null){
                result.append(lineText);
            }
            lineReader.close();
        }catch (IOException e){
            e.printStackTrace();
        }
        return result.toString();
    }

}
package de.tum.i13.ECSTest;

import de.tum.i13.ECS.ECS;
import de.tum.i13.ECS.Index;
import de.tum.i13.shared.DataMap;
import de.tum.i13.shared.Metadata;
import de.tum.i13.shared.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import static de.tum.i13.shared.Utility.byteToHex;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestConnection {

    private static Thread ecs;

    private static void launchECS() throws InterruptedException {
        ECS ex = new ECS(50309, "OFF");
        ecs = new Thread(ex);
        ecs.start();
        while(!ex.isReady()){
            Thread.sleep(1);
        }

    }


    @BeforeAll
    static void before() throws IOException, InterruptedException {
        launchECS();
    }

    @Test
    static void testSendMetadata() {
        try {
            Index index = new Index();
            String hash = computeHash();
            index.addServer(hash, "127.0.0.1", 10000, 8000);

            Metadata m = index.generateMetadata();
            ArrayList<Pair<String, DataMap>> originalMetadata = m.getAll();

            Socket inv = new Socket("127.0.0.1",50309);
            String toSend = "newServer " +
                    "127.0.0.1" +
                    " " +
                    8000 + " " + 10000;
            long len = toSend.length();
            ObjectOutputStream ECSoos = new ObjectOutputStream(new BufferedOutputStream(inv.getOutputStream()));
            ECSoos.flush();
            ObjectInputStream ECSois = new ObjectInputStream(new BufferedInputStream(inv.getInputStream()));

            write(len, toSend, ECSoos);

            Metadata md = (Metadata) ECSois.readObject();
            ArrayList<Pair<String, DataMap>> receivedMetadata = md.getAll();
            assertEquals(originalMetadata.size(), receivedMetadata.size());
            for(int j = 0; j < originalMetadata.size(); j++){
                assertEquals(originalMetadata.get(j).getFirst(), receivedMetadata.get(j).getFirst());
                assertEquals(originalMetadata.get(j).getSecond().getIp(), receivedMetadata.get(j).getSecond().getIp());
                assertEquals(originalMetadata.get(j).getSecond().getPort(), receivedMetadata.get(j).getSecond().getPort());
                assertEquals(originalMetadata.get(j).getSecond().getStartIndex(), receivedMetadata.get(j).getSecond().getStartIndex());
                assertEquals(originalMetadata.get(j).getSecond().getEndIndex(), receivedMetadata.get(j).getSecond().getEndIndex());
            }
            ECSoos.close();
            ECSois.close();
            inv.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    @AfterAll
    static void afterAll(){
        ecs.interrupt();
    }


    private static void write(long len, String toSend, ObjectOutputStream oos) throws IOException {
        oos.writeLong(len);
        oos.writeUTF(toSend);
        oos.flush();
    }

    private static String computeHash(){
        MessageDigest md = null;
        try {
            StringBuilder str = new StringBuilder();
            str.append("127.0.0.1");
            str.append(8000);
            String toHash = str.toString();
            md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(toHash.getBytes());

            return byteToHex(messageDigest);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

}

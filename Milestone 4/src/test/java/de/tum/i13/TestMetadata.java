package de.tum.i13;

import de.tum.i13.ECS.Index;
import de.tum.i13.server.FileStorage.FileMap;
import de.tum.i13.server.FileStorage.rebalanceReturn;
import de.tum.i13.shared.DataMap;
import de.tum.i13.shared.Metadata;
import de.tum.i13.shared.Utility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static de.tum.i13.shared.Utility.byteToHex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


public class TestMetadata {
    private static Metadata metadata;

    @BeforeAll
    static void before() throws IOException {
        Index index = new Index();
        index.addServer(computeHash("127.0.0.1", 8000), "127.0.0.1", 10000, 8000, Utility.getFreePort());
        index.addServer(computeHash("127.0.0.1", 9000), "127.0.0.1", 10000, 9000, Utility.getFreePort());
        metadata = index.generateMetadata();
        String firstMetadata = "81672900c085fd542d0e316e795528b1,76633d5b16aa62326cb8954aa69255af,127.0.0.1:9000;76633d5b16aa62326cb8954aa69255b0,81672900c085fd542d0e316e795528b0,127.0.0.1:8000;\r\n";
        assertEquals(firstMetadata,metadata.toString());

        index.addServer(computeHash("127.0.0.1", 10000), "127.0.0.1", 10000, 10000, Utility.getFreePort());
        metadata = index.generateMetadata();
        String secondMetadata = "a19d8c2fb41d92826946e8c1fa3ae9e6,76633d5b16aa62326cb8954aa69255af,127.0.0.1:9000;76633d5b16aa62326cb8954aa69255b0,81672900c085fd542d0e316e795528b0,127.0.0.1:8000;81672900c085fd542d0e316e795528b1,a19d8c2fb41d92826946e8c1fa3ae9e5,127.0.0.1:10000;\r\n";
        assertEquals(secondMetadata, metadata.toString());
        String secondMetadataReplica = "a19d8c2fb41d92826946e8c1fa3ae9e6,76633d5b16aa62326cb8954aa69255af,127.0.0.1:9000;a19d8c2fb41d92826946e8c1fa3ae9e6,76633d5b16aa62326cb8954aa69255af,127.0.0.1:8000;a19d8c2fb41d92826946e8c1fa3ae9e6,76633d5b16aa62326cb8954aa69255af,127.0.0.1:10000;76633d5b16aa62326cb8954aa69255b0,81672900c085fd542d0e316e795528b0,127.0.0.1:8000;76633d5b16aa62326cb8954aa69255b0,81672900c085fd542d0e316e795528b0,127.0.0.1:10000;76633d5b16aa62326cb8954aa69255b0,81672900c085fd542d0e316e795528b0,127.0.0.1:9000;81672900c085fd542d0e316e795528b1,a19d8c2fb41d92826946e8c1fa3ae9e5,127.0.0.1:10000;81672900c085fd542d0e316e795528b1,a19d8c2fb41d92826946e8c1fa3ae9e5,127.0.0.1:9000;81672900c085fd542d0e316e795528b1,a19d8c2fb41d92826946e8c1fa3ae9e5,127.0.0.1:8000;\r\n";
        assertEquals(secondMetadataReplica, metadata.toStringReplicas());

    }

    @Test
    void test1(){
        assertEquals(9000,metadata.getResponsible(computeHash("127.0.0.1", 9000)).getSecond());
        assertEquals(8000,metadata.getReplica(computeHash("127.0.0.1", 9000), 1).getSecond());
        assertEquals(10000,metadata.getReplica(computeHash("127.0.0.1", 9000), 2).getSecond());
    }

    @Test
    void test2(){
        assertEquals(10000,metadata.getResponsible(computeHash("127.0.0.1", 10000)).getSecond());
        assertEquals(9000,metadata.getReplica(computeHash("127.0.0.1", 10000), 1).getSecond());
        assertEquals(8000,metadata.getReplica(computeHash("127.0.0.1", 10000), 2).getSecond());
    }

    @Test
    void test3(){
        assertEquals(9000,metadata.getResponsible("00000000000000000000000000000000").getSecond());
        assertEquals(8000,metadata.getReplica("00000000000000000000000000000000", 1).getSecond());
        assertEquals(10000,metadata.getReplica("00000000000000000000000000000000", 2).getSecond());
    }

    @Test
    void test4(){
        assertEquals(9000,metadata.getResponsible("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF").getSecond());
        assertEquals(8000,metadata.getReplica("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 1).getSecond());
        assertEquals(10000,metadata.getReplica("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 2).getSecond());
    }

    @Test
    void testError(){
        assertNull(metadata.getReplica("00000000000000000000000000000000", 4).getSecond());
        assertNull(metadata.getReplica("00000000000000000000000000000000", 14).getFirst());
    }


    private static String computeHash(String ip, int port){
        MessageDigest md = null;
        try {
            StringBuilder str = new StringBuilder();
            str.append(ip);
            str.append(port);
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


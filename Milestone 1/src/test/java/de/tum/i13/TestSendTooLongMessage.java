package de.tum.i13;

import de.tum.i13.connectionHandler.ConnectionHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

import static de.tum.i13.shared.LogSetup.setupLogging;
import static org.junit.jupiter.api.Assertions.*;

public class TestSendTooLongMessage {

    private final static Logger LOGGER = Logger.getLogger(TestSendTooLongMessage.class.getName());
    private static ConnectionHandler connection = new ConnectionHandler(LOGGER);

    @BeforeAll
    public static void testConnection() {
        setupLogging("test.log", LOGGER);
        connection.open("131.159.52.23", 5153);
        assertTrue(connection.isConnected());
        String expected = "Connection to MSRG Echo server established: /131.159.52.23:5153\r\n";
        String rcv = connection.receive();
        assertEquals(rcv, expected);
    }

    @Test
    public void testTooLongMesssageSend(){
        File file = new File(System.getProperty("user.dir") + "/Test/testTOOLONG.txt");
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String s = null;
            StringBuilder sb = new StringBuilder();
            while ((s = br.readLine()) != null){
                sb.append(s);
            }
            sb.append("\r\n");
            String toSend = sb.toString();
            int size = connection.send(toSend);
            assertEquals(size, -1);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @AfterAll
    public static void testCloseConnection(){
        connection.quit();
        assertFalse(connection.isConnected());
    }
}

package de.tum.i13;

import de.tum.i13.connectionHandler.ConnectionHandler;
import org.junit.jupiter.api.*;

import java.io.*;
import java.util.logging.Logger;
import static de.tum.i13.shared.LogSetup.setupLogging;
import static org.junit.jupiter.api.Assertions.*;

public class TestSendVeryLongMessage {

    private final static Logger LOGGER = Logger.getLogger(TestSendVeryLongMessage.class.getName());
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
    public void testVeryLongMessageSend(){
        File file = new File(System.getProperty("user.dir") + "/Test/testLONG.txt");
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(file));
            String s;
            StringBuilder sb = new StringBuilder();
            while ((s = br.readLine()) != null){
                sb.append(s);
            }
            sb.append("\r\n");
            String toSend = sb.toString();
            int size = connection.send(toSend);
            br.close();
            assertEquals(size, toSend.getBytes().length);
            String message = connection.receive(size);
            assertEquals(message, toSend);
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

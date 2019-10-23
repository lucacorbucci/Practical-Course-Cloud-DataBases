package de.tum.i13;

import de.tum.i13.connectionHandler.ConnectionHandler;
import org.junit.jupiter.api.*;
import java.util.logging.Logger;
import static de.tum.i13.shared.LogSetup.setupLogging;
import static org.junit.jupiter.api.Assertions.*;


public class TestSendLongMessage {

    private final static Logger LOGGER = Logger.getLogger(TestSendLongMessage.class.getName());
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
    public void testLongMesssageSend(){
        String toSend = "\"Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.\"\r\n";
        int size = connection.send(toSend);
        assertEquals(size, toSend.getBytes().length);
        String message = connection.receive(size);
        assertEquals(message, toSend);
    }

    @AfterAll
    public static void testCloseConnection(){
        connection.quit();
        assertFalse(connection.isConnected());
    }
}
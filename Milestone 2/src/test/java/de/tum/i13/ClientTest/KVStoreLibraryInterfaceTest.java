package de.tum.i13.ClientTest;

import de.tum.i13.Util;
import de.tum.i13.client.ActiveConnection;
import de.tum.i13.client.KVStoreLibrary;
import de.tum.i13.server.kv.KVCommandProcessor;
import de.tum.i13.server.kv.KVStore;
import de.tum.i13.server.nio.NioServer;
import de.tum.i13.shared.CommandProcessor;
import de.tum.i13.shared.Constants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class KVStoreLibraryInterfaceTest {

    public static Integer port = 5153;
    private static NioServer sn;
    private static Thread th;
    public static Logger logger = Logger.getLogger(KVStoreLibraryInterfaceTest.class.getName());
    private static KVStoreLibrary kvs = new KVStoreLibrary(logger);
    private static PrintStream originalOut = System.out;
    private static PrintStream originalErr = System.err;
    private static ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private static ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    private static void launchServer(boolean test) throws IOException, InterruptedException {
        Path path = Paths.get("data/");
        if (!Files.exists(path)) {
            File dir = new File(path.toString());
            dir.mkdir();
        }
        port = Util.getFreePort();
        CommandProcessor server = new KVCommandProcessor(new KVStore(3, Constants.FIFO, path,test));
        sn = new NioServer(server);
        sn.bindSockets("127.0.0.1", port);
        th = new Thread() {
            @Override
            public void run() {
                try {
                    sn.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        th.start();
        Thread.sleep(2000);
    }

    @BeforeAll
    public static void beforAll() throws IOException, InterruptedException {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        launchServer(true);
    }

    @Test
    // Put a key,value pair and get it from the server
    public void put(){
        ActiveConnection ac = connectToServer();

        kvs.putKV(ac, new String[]{"put","Hello", "World"});
        assertEquals("EchoClient> put_success Hello\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get", "Hello"});
        assertEquals("EchoClient> get_success Hello World\n", outContent.toString());
        outContent.reset();

        kvs.closeConnection(ac);
    }

    @Test
    public void putTooLongKey(){
        ActiveConnection ac = connectToServer();

        kvs.putKV(ac, new String[]{"put","ewewewrwerewewewrwerewewewrwerewewewrwerewewewrwer", "World"});
        assertEquals(String.format("EchoClient> key must be less than %s bytes, value less than %s\n" , Constants.KEY_MAX_LENGTH, Constants.VALUE_MAX_LENGTH), outContent.toString());
        outContent.reset();
        kvs.closeConnection(ac);
    }

    @Test
    public void putUpdate(){

        ActiveConnection ac = connectToServer();

        kvs.putKV(ac, new String[]{"put","AAAA", "World"});
        assertEquals("EchoClient> put_success AAAA\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get", "AAAA"});
        assertEquals("EchoClient> get_success AAAA World\n", outContent.toString());
        outContent.reset();

        kvs.putKV(ac, new String[]{"put","AAAA", "Ciao"});
        assertEquals("EchoClient> put_update AAAA\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get", "AAAA"});
        assertEquals("EchoClient> get_success AAAA Ciao\n", outContent.toString());
        outContent.reset();

        kvs.closeConnection(ac);
    }

    @Test
    public void putError(){
        ActiveConnection ac = connectToServer();
        kvs.putKV(ac, new String[]{"put"});
        assertEquals("EchoClient> Unknown command\n", outContent.toString());
        outContent.reset();

        kvs.closeConnection(ac);

        kvs.putKV(ac, new String[]{"put","Ciao", "World"});
        assertEquals("EchoClient> Error! Not connected!\n", outContent.toString());
        outContent.reset();

    }

    @Test
    public void putSpace(){
        ActiveConnection ac = connectToServer();
        kvs.putKV(ac, new String[]{"put","teeeeeest", "some thing"});
        assertEquals("EchoClient> put_success teeeeeest\n", outContent.toString());
        outContent.reset();
        kvs.getValue(ac, new String[]{"get", "teeeeeest"});
        assertEquals("EchoClient> get_success teeeeeest some thing\n", outContent.toString());
        outContent.reset();

        kvs.closeConnection(ac);
    }

    @Test
    public void get(){
        ActiveConnection ac = connectToServer();

        kvs.putKV(ac, new String[]{"put","qwerty", "World"});
        assertEquals("EchoClient> put_success qwerty\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get", "qwerty"});
        assertEquals("EchoClient> get_success qwerty World\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get", "notAKey"});
        assertEquals("EchoClient> get_error notAKey key not found.\n", outContent.toString());
        outContent.reset();

        kvs.closeConnection(ac);

    }

    @Test
    public void getCarriageReturn(){
        ActiveConnection ac = connectToServer();
        kvs.putKV(ac, new String[]{"put","carriageReturn", "some\r\nthing"});
        assertEquals("EchoClient> put_success carriageReturn\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get", "carriageReturn"});
        assertEquals("EchoClient> get_success carriageReturn some\r\nthing\n", outContent.toString());
        outContent.reset();

        kvs.closeConnection(ac);
    }


    @Test
    public void delete(){
        ActiveConnection ac = connectToServer();

        kvs.putKV(ac, new String[]{"put","Halo", "World"});
        assertEquals("EchoClient> put_success Halo\n", outContent.toString());
        outContent.reset();

        kvs.putKV(ac, new String[]{"put","test2", "some\r\nthing"});
        assertEquals("EchoClient> put_success test2\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get", "Halo"});
        assertEquals("EchoClient> get_success Halo World\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get", "test2"});
        assertEquals("EchoClient> get_success test2 some\r\nthing\n", outContent.toString());
        outContent.reset();

        kvs.putKV(ac, new String[]{"put","test2"});
        assertEquals("EchoClient> delete_success test2\n", outContent.toString());
        outContent.reset();

        kvs.putKV(ac, new String[]{"delete","Halo"});
        assertEquals("EchoClient> delete_success Halo\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get", "Halo"});
        assertEquals("EchoClient> get_error Halo key not found.\n", outContent.toString());
        outContent.reset();
        kvs.getValue(ac, new String[]{"get", "test2"});
        assertEquals("EchoClient> get_error test2 key not found.\n", outContent.toString());
        outContent.reset();

        kvs.closeConnection(ac);
    }

    private ActiveConnection connectToServer(){
        ActiveConnection ac = kvs.buildConnection(new String[]{"connect","127.0.0.1", port.toString()});
        assertNotNull(ac);
        assertEquals("EchoClient> Connection to database server established: /127.0.0.1:" + port.toString() + "\n", outContent.toString());
        outContent.reset();
        return ac;
    }


    @AfterAll
    public static void afterAll() {
        th.interrupt();
        sn.close();
        Path path = Paths.get("data/");
        File[] files = new File(path.toAbsolutePath().toString() +"/").listFiles();
        for(File file: files){
            file.delete();
        }
    }
}
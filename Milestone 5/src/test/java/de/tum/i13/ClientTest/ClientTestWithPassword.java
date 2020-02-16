package de.tum.i13.ClientTest;

import de.tum.i13.ECS.ECS;
import de.tum.i13.client.ActiveConnection;
import de.tum.i13.client.KVStoreLibrary;
import de.tum.i13.server.kv.KVCommandProcessor;
import de.tum.i13.server.kv.KVStore;
import de.tum.i13.server.nio.NioServer;
import de.tum.i13.shared.Config;
import de.tum.i13.shared.Constants;
import de.tum.i13.shared.ServerStatus;
import de.tum.i13.shared.inputPassword;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class ClientTestWithPassword {

    public static Integer port1 = 5159;
    public static Integer port2 = 6000;
    public static Logger logger = Logger.getLogger(KVStoreLibraryInterfaceTest.class.getName());
    private static KVStoreLibrary kvs = new KVStoreLibrary(logger, new inputPassword(false, 0));
    private static ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private static ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    private static KVStore kv1;
    private static KVCommandProcessor cmdp1;
    private static KVCommandProcessor cmdp2;
    private static KVStore kv2;
    private static Thread th1;
    private static Thread th2;
    private static NioServer ns1;
    private static NioServer ns2;
    private static Thread ecs;
    private static ServerStatus ss1 = new ServerStatus(Constants.INACTIVE);
    private static ServerStatus ss2 = new ServerStatus(Constants.INACTIVE);

    private static void launchServer(boolean test) throws IOException, InterruptedException {
        ECS ex = new ECS(51357, "ALL");
        ecs = new Thread(ex);
        ecs.start();
        while (!ex.isReady()) {
            Thread.sleep(1);
        }

        String[] args1 = {"-s", "FIFO", "-c", "100", "-d", "data/", "-l", "logM.log", "-ll", "ALL", "-b", "127.0.0.1:51357", "-p", String.valueOf(port1)};
        Config cfg1 = Config.parseCommandlineArgs(args1);

        kv1 = new KVStore(cfg1, true, ss1);
        cmdp1 = new KVCommandProcessor(kv1);
        ns1 = new NioServer(cmdp1);

        String[] args2 = {"-s", "FIFO", "-c", "100", "-d", "data2/", "-l", "logM2.log", "-ll", "ALL", "-b", "127.0.0.1:51357", "-p", String.valueOf(port2)};
        Config cfg2 = Config.parseCommandlineArgs(args2);
        kv2 = new KVStore(cfg2, true, ss2);
        cmdp2 = new KVCommandProcessor(kv2);
        ns2 = new NioServer(cmdp2);

        try {

            ns1.bindSockets(cfg1.listenaddr, cfg1.port);
            ns2.bindSockets(cfg2.listenaddr, cfg2.port);

            th1 = new Thread(() -> {
                try {
                    ns1.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            th1.start();
            while (ss1.checkEqual(Constants.INACTIVE)) {
                Thread.sleep(1);
            }

            th2 = new Thread(() -> {
                try {
                    ns2.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            th2.start();
            while (ss2.checkEqual(Constants.INACTIVE)) {
                Thread.sleep(1);
            }
        } catch (IOException e) {
            System.out.println("failed");
            launchServer(true);
        }


    }

    @BeforeAll
    public static void beforeAll() throws IOException, InterruptedException {
        Path path = Paths.get("data/");
        File[] files = new File(path.toAbsolutePath().toString() + "/").listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        Path path2 = Paths.get("data2/");
        File[] files2 = new File(path2.toAbsolutePath().toString() + "/").listFiles();
        if (files2 != null) {
            for (File file : files2) {
                file.delete();
            }
        }
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        launchServer(true);
    }

    @Test
    public void put() {
        ActiveConnection ac = connectToServer(port1);

        kvs.putKVWithPassword(ac, new String[]{"PutWithPassword", "127.0.0.1" + port1, "World1"}, "Pippo");
        assertEquals("EchoClient> put_success 127.0.0.15159\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get ", "127.0.0.1" + port1});
        assertEquals("EchoClient> Input Password: \n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get ", "127.0.0.1" + port1}, "Pluto");
        assertEquals("EchoClient> Input Password: \n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get ", "127.0.0.1" + port1}, "Pippo");
        assertEquals("EchoClient> get_success 127.0.0.1" + port1 + " World1\n", outContent.toString());
        outContent.reset();


        kvs.putKVWithPassword(ac, new String[]{"PutWithPassword", "127.0.0.1" + port2, "World1"}, "Ciao");
        assertEquals("EchoClient> put_success 127.0.0.1" + port2 + "\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get ", "127.0.0.1" + port2});
        assertEquals("EchoClient> Input Password: \n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get ", "127.0.0.1" + port2}, "Pluto");
        assertEquals("EchoClient> Input Password: \n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get ", "127.0.0.1" + port2}, "Ciao");
        assertEquals("EchoClient> get_success 127.0.0.1" + port2 + " World1\n", outContent.toString());
        outContent.reset();


        while (!ss1.checkEqual(Constants.ACTIVE) && ss1.checkEqual(Constants.ACTIVE)) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            ac.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ActiveConnection ac1 = connectToServer(port1);

        kvs.getValue(ac1, new String[]{"get ", "127.0.0.1" + port1});
        assertEquals("EchoClient> Input Password: \n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac1, new String[]{"get ", "127.0.0.1" + port1}, "Pluto");
        assertEquals("EchoClient> Input Password: \n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac1, new String[]{"get ", "127.0.0.1" + port1}, "Pippo");
        assertEquals("EchoClient> get_success 127.0.0.1" + port1 + " World1\n", outContent.toString());
        outContent.reset();

        try {
            ac1.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ActiveConnection ac2 = connectToServer(port1);

        kvs.getValue(ac2, new String[]{"get ", "127.0.0.1" + port2});
        assertEquals("EchoClient> Input Password: \n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac2, new String[]{"get ", "127.0.0.1" + port2}, "Pluto");
        assertEquals("EchoClient> Input Password: \n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac2, new String[]{"get ", "127.0.0.1" + port2}, "Ciao");
        assertEquals("EchoClient> get_success 127.0.0.1" + port2 + " World1\n", outContent.toString());
        outContent.reset();

        kvs.putKVWithPassword(ac, new String[]{"PutWithPassword", "127.0.0.1" + port2}, "Ciao");
        assertEquals("EchoClient> delete_success 127.0.0.1" + port2 + "\n", outContent.toString());
        outContent.reset();

        kvs.closeConnection(ac);
        kvs.closeConnection(ac1);
        kvs.closeConnection(ac2);

    }

    @Test
    void addUpdateAndDelete() {
        ActiveConnection ac = connectToServer(port1);

        // Add and get
        kvs.putKVWithPassword(ac, new String[]{"PutWithPassword", "HelloMoto", "World1"}, "Pippo");
        assertEquals("EchoClient> put_success HelloMoto\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get ", "HelloMoto"}, "Pippo");
        assertEquals("EchoClient> get_success HelloMoto" + " World1\n", outContent.toString());
        outContent.reset();

        // Update and Get
        kvs.putKVWithPassword(ac, new String[]{"PutWithPassword", "HelloMoto", "World2"}, "Pippo");
        assertEquals("EchoClient> put_update HelloMoto\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get ", "HelloMoto"}, "Pippo");
        assertEquals("EchoClient> get_success HelloMoto" + " World2\n", outContent.toString());
        outContent.reset();

        // Update using put and Get
        kvs.putKVWithPassword(ac, new String[]{"put", "HelloMoto", "World3"}, "Pippo");
        assertEquals("EchoClient> put_update HelloMoto\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get ", "HelloMoto"}, "Pippo");
        assertEquals("EchoClient> get_success HelloMoto" + " World3\n", outContent.toString());
        outContent.reset();

        // Delete using putWithPassword
        kvs.putKVWithPassword(ac, new String[]{"PutWithPassword", "HelloMoto"}, "Pippo");
        assertEquals("EchoClient> delete_success HelloMoto\n", outContent.toString());
        outContent.reset();

        // Add, get and remove using put
        kvs.putKVWithPassword(ac, new String[]{"PutWithPassword", "HelloWorld", "World5"}, "Pippo");
        assertEquals("EchoClient> put_success HelloWorld\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get ", "HelloWorld"}, "Pippo");
        assertEquals("EchoClient> get_success HelloWorld" + " World5\n", outContent.toString());
        outContent.reset();

        kvs.putKVWithPassword(ac, new String[]{"put", "HelloWorld"}, "Pippo");
        assertEquals("EchoClient> delete_success HelloWorld\n", outContent.toString());
        outContent.reset();

        kvs.closeConnection(ac);
    }

    @Test
    void testWithWrongPasswords() {
        ActiveConnection ac = connectToServer(port1);

        // Add and get
        kvs.putKVWithPassword(ac, new String[]{"PutWithPassword", "Hello1", "World1"}, "Pippo");
        assertEquals("EchoClient> put_success Hello1\n", outContent.toString());
        outContent.reset();

        kvs.getValue(ac, new String[]{"get ", "Hello1"}, "Pippo");
        assertEquals("EchoClient> get_success Hello1 World1\n", outContent.toString());
        outContent.reset();

        // delete
        kvs.putKVWithPassword(ac, new String[]{"put", "Hello1"});
        assertEquals("EchoClient> Input Password: \n", outContent.toString());
        outContent.reset();

        kvs.putKVWithPassword(ac, new String[]{"put", "Hello1"}, "ciao");
        assertEquals("EchoClient> Input Password: \n", outContent.toString());
        outContent.reset();

        kvs.putKVWithPassword(ac, new String[]{"PutWithPassword", "Hello1"}, "Pluto");
        assertEquals("EchoClient> Input Password: \n", outContent.toString());
        outContent.reset();

        kvs.putKVWithPassword(ac, new String[]{"PutWithPassword", "Hello1"}, "Pluto");
        assertEquals("EchoClient> Input Password: \n", outContent.toString());
        outContent.reset();

        kvs.closeConnection(ac);

    }


    private ActiveConnection connectToServer(int port) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        ActiveConnection ac = null;

        while (ac == null) {
            try {
                ac = kvs.buildConnection(new String[]{"connect", "127.0.0.1", String.valueOf(port)});
            } catch (Exception e) {
                baos.reset();
            }
        }


        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        assertNotNull(ac);
        outContent.reset();
        return ac;
    }


    @AfterAll
    public static void afterAll() {
        th1.interrupt();
        th2.interrupt();
        ecs.interrupt();
        ns1.close();
        ns2.close();

        Path path = Paths.get("data/");
        File[] files = new File(path.toAbsolutePath().toString() + "/").listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        Path path2 = Paths.get("data2/");
        File[] files2 = new File(path2.toAbsolutePath().toString() + "/").listFiles();

        if (files2 != null) {
            for (File file : files2) {
                file.delete();
            }
        }
    }
}

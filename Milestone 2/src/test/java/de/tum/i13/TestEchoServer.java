package de.tum.i13;


import de.tum.i13.server.echo.EchoLogic;
import de.tum.i13.server.nio.NioServer;
import de.tum.i13.shared.CommandProcessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static de.tum.i13.Util.getFreePort;
import static de.tum.i13.shared.LogSetup.changeLoglevel;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestEchoServer {

    public static Integer port = 5153;
    private static NioServer sn;
    private static Thread th;
    /*
    @BeforeAll
    public static void beforAll() throws IOException, InterruptedException {
        port = getFreePort();

        CommandProcessor echoLogic = new EchoLogic();
        sn = new NioServer(echoLogic);
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

        changeLoglevel(Level.OFF);
    }

    @AfterAll
    public static void afterAll() {
        th.interrupt();
        sn.close();
    }

    @Test
    public void smokeTest() throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress("127.0.0.1", port));

        PrintWriter output = new PrintWriter(s.getOutputStream());
        BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));


        String welcome = input.readLine();
        assertThat(welcome, is(containsString("Connection to MSRG Echo server established:")));

        //send a single command
        String command = "hello ";
        output.write(command + "\r\n");
        output.flush();

        //read response
        String res = input.readLine();

        assertThat(command, is(equalTo(res)));

        //close everything
        s.close();
    }


    @Test
    public void multipleCommandsSeparated() throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress("127.0.0.1", port));
        PrintWriter output = new PrintWriter(s.getOutputStream());
        BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));

        String welcome = input.readLine();
        assertThat(welcome, is(containsString("Connection to MSRG Echo server established:")));

        //send multiple commands in one flush
        String command = "hello\r\necho\r\n";

        output.write(command);
        output.flush();

        String res = input.readLine();
        assertThat(res, is(equalTo("hello")));

        res = input.readLine();
        assertThat(res, is(equalTo("echo")));

        //finishing up
        s.close();
    }

    @Test
    public void multipleCommandsBatched() throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress("127.0.0.1", port));
        PrintWriter output = new PrintWriter(s.getOutputStream());
        BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));

        String welcome = input.readLine();
        assertThat(welcome, is(containsString("Connection to MSRG Echo server established:")));

        int howMany = 100;

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < howMany; i++) {
            sb.append("echo" + i + "\r\n");
        }
        output.write(sb.toString());
        output.flush();

        for(int i = 0; i < howMany; i++) {
            assertThat(input.readLine(), is(equalToObject("echo" + i)));
        }

        //finishing up
        s.close();
    }


    @Test
    public void enjoyTheEcho() throws InterruptedException {
        int numberOfThreads = 5;
        int numberOfRequests = 10000;
        final int numberOfErrors = 0;
        CountDownLatch cdl = new CountDownLatch(numberOfThreads);

        for (int tcnt = 0; tcnt < numberOfThreads; tcnt++){
            final int finalTcnt = tcnt;
            new Thread(() -> {
                try {
                    for(int i = 0; i < numberOfRequests; i++) {
                        Socket s = new Socket();
                        s.connect(new InetSocketAddress("127.0.0.1", port));

                        PrintWriter output = new PrintWriter(s.getOutputStream());
                        BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));

                        input.readLine(); //Welcome message

                        String command = "hello " + finalTcnt;
                        output.write(command + "\r\n");
                        output.flush();
                        String ret = input.readLine();

                        assertThat(command, is(equalTo(ret)));
                        s.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                cdl.countDown();
            }).start();
        }

        cdl.await(1, TimeUnit.MINUTES);
        assertThat("Not finished, too slow", cdl.getCount(), is(0L));

        assertThat(numberOfErrors, is(0));
    }
*/
}

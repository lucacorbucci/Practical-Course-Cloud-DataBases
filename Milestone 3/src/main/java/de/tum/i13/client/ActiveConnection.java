package de.tum.i13.client;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Logger;

import static de.tum.i13.shared.LogSetup.setupLogging;
/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class ActiveConnection implements AutoCloseable {
    public static Logger logger = Logger.getLogger(ActiveConnection.class.getName());

    private Socket socket;
    private PrintWriter output;
    private BufferedReader input;

    public ActiveConnection(Socket socket, PrintWriter output, BufferedReader input) {
        setupLogging("OFF");
        this.socket = socket;
        this.output = output;
        this.input = input;
    }

    public ActiveConnection(){
        this.socket = null;
        this.output = null;
        this.input = null;
    }

    public void write(String command) {
        output.write(command + "\r\n");
        output.flush();
    }

    public String readline() throws IOException {
        return input.readLine();
    }

    public void close() throws Exception {
        output.close();
        input.close();
        socket.close();
    }

    public void reconnect(String ip, int port)  {
        try {
            output.close();
            input.close();
            socket.close();
        } catch (IOException e) {
            logger.warning("Error while closing old connection.");
        }
        try {
            socket = new Socket(ip, port);
            output = new PrintWriter(socket.getOutputStream());
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public String getIp(){
        return socket.getInetAddress().toString().substring(1);
    }

    public int getPort(){
        return socket.getPort();
    }

    public String getInfo() {
        return "/" + this.socket.getRemoteSocketAddress().toString();
    }
}

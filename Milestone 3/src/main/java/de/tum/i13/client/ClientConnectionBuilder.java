package de.tum.i13.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class ClientConnectionBuilder {

    private final String host;
    private final int port;

    public ClientConnectionBuilder(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public ActiveConnection connect() throws IOException {
        Socket s = null;
        s = new Socket(this.host, this.port);

        PrintWriter output = null;

        output = new PrintWriter(s.getOutputStream());

        output.flush();
        BufferedReader input = null;
        input = new BufferedReader(new InputStreamReader(s.getInputStream()));

        return new ActiveConnection(s, output, input);
    }
}

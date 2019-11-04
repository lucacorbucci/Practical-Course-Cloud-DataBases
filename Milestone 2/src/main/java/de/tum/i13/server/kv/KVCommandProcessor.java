package de.tum.i13.server.kv;

import de.tum.i13.server.nio.StartNioServer;
import de.tum.i13.shared.CommandProcessor;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.logging.Level;

public class KVCommandProcessor implements CommandProcessor {
    private KVStore kvStore;

    public KVCommandProcessor(KVStore kvStore) {
        this.kvStore = kvStore;
        StartNioServer.logger.setLevel(Level.ALL);
    }

    /**
     * Fetch the value corresponding to the given key from the kv-storage
     * @param cmds holds the user input
     * @return the given value if stored
     */
    private String get(String[] cmds){
        if(cmds.length == 2) { //Process Get Command
            StartNioServer.logger.info("processing GET: " + cmds[1]);
            String command = kvStore.get(cmds[1]);
            if(command != null) {
                StartNioServer.logger.fine("GET succes: " + cmds[1]+ " " + command);
                return "get_success " + cmds[1] + " " + command;
            } else {
                StartNioServer.logger.fine("GET error: " + cmds[1]);
                return "get_error " + cmds[1] + " key not found.";
            }
        } else {
        	return "get_error wrong amount of parameters.";
        }
    }

    /**
     * inserts/updates a key value pair in the kv-storage
     * @param cmds holds the user input
     * @return a message if the task was fullfilled
     */
    private String put(String[] cmds) {

        if(cmds.length < 3){
            return "put_error wrong number of parameters";
        }

        StartNioServer.logger.info("processing PUT: " + cmds[1]);
        // This stringbuilder is necessary if we use another
        // client. If this client uses a different encoding maybe it can
        // sends more than 1 string as a value
        StringBuilder v = new StringBuilder();
        for (int i = 2; i < cmds.length; i++) {
            v.append(cmds[i]);
        }
        String value = v.toString();
        try {
            //
            int ret = kvStore.put(cmds[1], value);
            if (ret == 0) {
                // NEW DATA
                StartNioServer.logger.fine("PUT success: " + cmds[1] + " " + value);
                return "put_success " + cmds[1];
            } else if (ret == 1) {
                /// UPDATE
                StartNioServer.logger.fine("PUT update: " + cmds[1] + " " + value);
                return "put_update " + cmds[1];
            } else  {
                //ERROR
            	StartNioServer.logger.fine("PUT error: " + cmds[1] + " " + value);
                return "put_error " + cmds[1];
            }
        } catch (Exception e) {
            StartNioServer.logger.fine("PUT error: " + cmds[1] + " " + value);
            return "put_error " + cmds[1] + " " + cmds[2] ;
        }
    }


    /**
     * deletes a key value pair from the kv-storage
     * @param cmds holds the user input
     * @return 's the success or error message
     */
    private String delete(String[] cmds){
        if(cmds.length == 2) {
            StartNioServer.logger.info("processing DELETE: " + cmds[1]);
            int command = kvStore.delete(cmds[1]);

            if(command == 1) {
                StartNioServer.logger.fine("DELETE success: " + cmds[1]);
                return "delete_success " + cmds[1];
            } else {
                StartNioServer.logger.fine("DELETE error: " + cmds[1] + " not found.");
                return "delete_error " + cmds[1];
            }
        }
        StartNioServer.logger.fine("PUT error: " + cmds[1]);
        return "put_error (delete) wrong amount of parameters.";
    }

    /**
     * processes the user input and calls the corresponding function to execute the task
     * @param command holds the user input
     * @return s the state of the action that has been taken(put, get,....,success..)
     */
    @Override
    public String process(String command) {

        String[] request = command.split(" ");
        int size = request.length;
        request[size-1] = request[size-1].replace("\r\n", "");
        request[0] = request[0].toLowerCase();
        switch (request[0]) {
            case "put":
                return put(request);
            case "delete":
                return delete(request);
            case "get":
                return get(request);
            default:
                StartNioServer.logger.info("Error: Wrong command.");
                return "Error. Wrong command.";
        }

    }


    @Override
    public String connectionAccepted(InetSocketAddress address, InetSocketAddress remoteAddress) {
        StartNioServer.logger.info("new connection: " + remoteAddress.toString());

        return "Connection to database server established: " + address.toString() + "\r\n";
    }

    @Override
    public void connectionClosed(InetAddress address) {
        StartNioServer.logger.info("connection closed: " + address.toString());
    }
}

package de.tum.i13.server.kv;

import de.tum.i13.shared.CommandProcessor;
import de.tum.i13.shared.Constants;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

import static de.tum.i13.shared.LogSetup.setupLogging;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class KVCommandProcessor implements CommandProcessor {
    private KVStore kvStore;
    private Logger logger;

    public KVCommandProcessor(KVStore kvStore) {
        this.kvStore = kvStore;
        try {
            this.logger = kvStore.getLogger();
            if (this.logger == null) {
                throw new NullPointerException();
            }
        } catch (NullPointerException e) {
            logger = Logger.getLogger(KVCommandProcessor.class.getName());
            setupLogging("OFF", logger);
        }
    }

    /**
     * processes the user input and calls the corresponding function to execute the
     * task
     *
     * @param command holds the user input
     * @return s the state of the action that has been taken(put,
     * get,....,success..)
     */
    @Override
    public String process(String command) {
        try {

            logger.info("Processing: " + command);
            String[] request = command.split(" ");
            int size = request.length;
            request[size - 1] = request[size - 1].replace("\r\n", "");
            request[0] = request[0].toLowerCase();
            logger.info("Processing: " + command);
            switch (request[0]) {
                case "put":
                    return put(request);
                case "delete":
                    return delete(request);
                case "get":
                    return get(request);
                case "keyrange":
                    return keyrange();
                case "keyrange_read":
                    return keyrange_read();
                default:
                    logger.info("Error: Wrong command.");
                    return "Error. Wrong command.";
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void simulateSIGKILL() {
        kvStore.simulateSIGKILL();
    }

    /**
     * Fetch the value corresponding to the given key from the kv-storage
     *
     * @param cmds holds the user input
     * @return the given value if stored
     */
    private String get(String[] cmds) {
        if (cmds.length == 2) { //Process Get Command
            logger.info("processing GET: " + cmds[1]);
            String command = kvStore.get(cmds[1]);
            if (command == null) {
                return handleReply(Constants.GET_ERROR + " " + cmds[1] + " key not found.");
            } else if (command.equals(Constants.NOTRESPONSIBLE)) {
                return handleReply(Constants.NOTRESPONSIBLE);
            } else if (command.equals(Constants.SERVERSTOPPED)) {
                return handleReply(Constants.SERVERSTOPPED);
            } else if (command.equals(Constants.WRITELOCK)) {
                return handleReply(Constants.WRITELOCK);
            } else {
                return handleReply(Constants.GET_SUCCESS + " " + cmds[1] + " " + command);
            }
        } else {
            return "get_error wrong amount of parameters.";
        }
    }


    /**
     * inserts/updates a key value pair in the kv-storage
     *
     * @param cmds holds the user input
     * @return a message if the task was fullfilled
     */
    private String put(String[] cmds) {
        if (cmds.length < 3) {
            return "put_error wrong number of parameters";
        }
        logger.info("processing PUT: " + cmds[1]);
        String value = buildString(cmds);

        try {
            int ret = kvStore.put(cmds[1], value);
            if (ret == Constants.PUT_SUCCESS) {
                return handleReply(Constants.PUTSUCCESS + " " + cmds[1]);
            } else if (ret == Constants.PUT_UPDATE) {
                return handleReply(Constants.PUTUPDATE + " " + cmds[1]);
            } else if (ret == Constants.SERVER_NOT_RESPONSIBLE) {
                return handleReply(Constants.NOTRESPONSIBLE);
            } else if (ret == Constants.SERVER_STOPPED) {
                return handleReply(Constants.SERVERSTOPPED);
            } else if (ret == Constants.WRITE_LOCK) {
                return handleReply(Constants.WRITELOCK);
            } else {
                return handleReply(Constants.ERRORPUT + " " + cmds[1]);
            }
        } catch (Exception e) {
            return handleReply(Constants.ERRORPUT + " " + cmds[1]);
        }
    }

    /**
     * @param cmds
     * @return
     */
    private String buildString(String[] cmds) {

        StringBuilder v = new StringBuilder();
        for (int i = 2; i < cmds.length; i++) {
            v.append(cmds[i]);
            if (i + 1 < cmds.length) {
                v.append(" ");
            }
        }
        return v.toString();
    }

    /**
     * @param reply
     * @return
     */
    private String handleReply(String reply) {
        logger.fine(reply);
        return reply;
    }


    /**
     * deletes a key value pair from the kv-storage
     *
     * @param cmds holds the user input
     * @return 's the success or error message
     */
    private String delete(String[] cmds) {
        if (cmds.length == 2) {
            logger.info("processing DELETE: " + cmds[1]);
            int command = kvStore.delete(cmds[1]);

            if (command == Constants.DELETE_OK) {
                return handleReply(Constants.DELETE_SUCCESS + " " + cmds[1]);
            } else if (command == Constants.DELETE_ERROR) {
                return handleReply(Constants.DELETE_ERR + " " + cmds[1]);
            } else if (command == Constants.DELETE_NOT_RESP) {
                return handleReply(Constants.DELETE_NOTRESP);
            } else if (command == Constants.DELETE_STOPPED) {
                return handleReply(Constants.DELETE_STOP);
            } else if (command == Constants.DELETE_WRITE_LOCK) {
                return handleReply(Constants.DELETE_WR_LOCK);
            }
        }
        logger.fine("PUT error: " + cmds[1]);
        return "put_error (delete) wrong amount of parameters.";
    }

    /**
     * Keyrange of the KVServers
     *
     * @return the keyrange from all the used servers or just the responsible server
     * of a given hash
     */
    public String keyrange() {
        return kvStore.getKeyRange();
    }

    /**
     * Replicas of the KVServers
     *
     * @return a String with all the replicas
     */
    public String keyrange_read() {
        return kvStore.getKeyRangeReplicas();
    }

    @Override
    public String connectionAccepted(InetSocketAddress address, InetSocketAddress remoteAddress) {
        logger.info("new connection: " + remoteAddress.toString());
        return "Connection to database server established: " + address.toString() + "\r\n";
    }

    @Override
    public void connectionClosed(InetAddress address) {
        logger.info("connection closed: " + address.toString());
    }
}

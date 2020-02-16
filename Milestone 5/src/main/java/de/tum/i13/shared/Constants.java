package de.tum.i13.shared;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class Constants {
    public static final String TELNET_ENCODING = "ISO-8859-1"; // encoding for telnet

    public static final String LRU = "LRU";
    public static final String LFU = "LFU";
    public static final String FIFO = "FIFO";
    public static final String GET_COMMAND = "get ";
    public static final String DELETE = "delete ";
    public static final String PUT = "put ";
    public static final String PUTPASS = "put_with_password ";
    public static final int KEY_MAX_LENGTH = 20;
    public static final int VALUE_MAX_LENGTH = 120000;
    public static final int MAX_FILE_SIZE = 1000;
    public static final String HEX_START_INDEX = "00000000000000000000000000000000";
    public static final String HEX_END_INDEX = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF";
    public static final String INACTIVE = "Inactive";
    public static final String ACTIVE = "Active";
    public static final String LOCKED = "Locked";
    public static final String NO_CONNECTION = "No_Connection";
    public static final int PUT_SUCCESS = 0;
    public static final int PUT_UPDATE = 1;
    public static final int SUB_SUCCESS = 0;
    public static final int UNSUB_SUCCESS = 1;
    public static final int SERVER_NOT_RESPONSIBLE = 3;
    public static final int SERVER_STOPPED = 4;
    public static final int WRITE_LOCK = 5;
    public static final int SHUTDOWN = -1;
    public static final int INVALID_PASSWORD = -3;

    public static final String PUTSUCCESS = "put_success";
    public static final String PUTUPDATE = "put_update";
    public static final String NOTRESPONSIBLE = "server_not_responsible";
    public static final String SERVERSTOPPED = "server_stopped";
    public static final String WRITELOCK = "server_write_lock";
    public static final String ERRORPUT = "put_error";
    public static final String INVALIDPASSWORD = "invalid_password";


    public static final String GET_ERROR = "get_error";
    public static final String GET_SUCCESS = "get_success";

    public static final String DELETE_SUCCESS = "delete_success";
    public static final String DELETE_NOTRESP = "server_not_responsible";
    public static final String DELETE_STOP = "server_stopped";
    public static final String DELETE_WR_LOCK = "server_write_lock";
    public static final String DELETE_ERR = "delete_error";


    public static final int DELETE_OK = 1;
    public static final int DELETE_ERROR = 0;
    public static final int DELETE_NOT_RESP = 2;
    public static final int DELETE_WRITE_LOCK = 4;
    public static final int DELETE_STOPPED = 3;
    public static final int ERROR = -1;
    public static final int DELETED = 2;
    public static final int NUM_REPLICAS = 2;

    public static final int DELETED_KV = -1;
    public static final int NEW_KV = 0;
    public static final int UPDATE_KV = 1;

    public static final int EVENTUAL_HASHING_TIMEOUT = 7000;



    public static final String TWO_PHASE_COMMIT = "2PC";
}


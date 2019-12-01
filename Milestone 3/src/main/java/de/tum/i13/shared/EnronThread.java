package de.tum.i13.shared;

import de.tum.i13.client.ActiveConnection;
import de.tum.i13.client.KVStoreLibrary;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class EnronThread implements Runnable{
    public static Logger logger = Logger.getLogger(Enron.class.getName());

    private ConcurrentLinkedQueue<Pair<String, String>> queue;
    private static KVStoreLibrary kvs = new KVStoreLibrary(logger);

    public EnronThread(ConcurrentLinkedQueue<Pair<String, String>> queue){
        this.queue = queue;
    }

    @Override
    public void run() {
        PrintStream stdout = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        boolean fine = false;

        while(!fine){
            Pair<String, String> pair = queue.poll();
            if(pair != null){
                if(pair.getFirst().equals("-111" )){
                    System.err.println("End: " + Thread.currentThread().getId());
                    fine = true;
                }
                else{
                    ActiveConnection ac = connectToServer(5000);

                    kvs.putKV(ac, new String[]{"put", trimByBytes(pair.getFirst().toString(), 20), trimByBytes(pair.getSecond(),120000)});
                    try {
                        ac.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        }
        System.err.println("FINE");
        System.setOut(stdout);
    }


    private static String trimByBytes(String str, int lengthOfBytes) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        if (lengthOfBytes < buffer.limit()) {
            buffer.limit(lengthOfBytes);
        }

        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.IGNORE);

        try {
            return decoder.decode(buffer).toString();
        } catch (CharacterCodingException e) {
            // We will never get here.
            throw new RuntimeException(e);
        }
    }


    private static ActiveConnection connectToServer(int port){

        ActiveConnection ac = null;
        ac = kvs.buildConnection(new String[]{"connect","127.0.0.1", String.valueOf(5000)});

        return ac;
    }

}

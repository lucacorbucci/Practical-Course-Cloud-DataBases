package de.tum.i13.server.nio;

import de.tum.i13.shared.CommandProcessor;
import de.tum.i13.shared.Constants;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

/**
 * Based on http://rox-xmlrpc.sourceforge.net/niotut/
 */
public class NioServer {

    private List<ChangeRequest> pendingChanges;
    private Map<SelectionKey, List<ByteBuffer>> pendingWrites;
    private Map<SelectionKey, byte[]> pendingReads;

    private Selector selector;
    private ServerSocketChannel serverChannel;

    private ByteBuffer readBuffer;
    private CommandProcessor cmdProcessor;

    public NioServer(CommandProcessor cmdProcessor) {
        this.cmdProcessor = cmdProcessor;
        this.pendingChanges = new LinkedList<>();
        this.pendingWrites = new HashMap<>();
        this.pendingReads = new HashMap<>();

        this.readBuffer = ByteBuffer.allocate(8192); // = 2^13
    }

    public void bindSockets(String servername, int port) throws IOException {
        // Create a new non-blocking server selectionKey channel
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.configureBlocking(false);

        // Bind the server selectionKey to the specified address and port
        InetSocketAddress isa = new InetSocketAddress(InetAddress.getByName(servername), port);
        this.serverChannel.socket().bind(isa);

        // Register the server selectionKey channel, indicating an interest in
        // accepting new connections
        this.selector = SelectorProvider.provider().openSelector();
        this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void start() throws IOException {
        while (true) {

            // Process queued interest changes
            for (ChangeRequest change : this.pendingChanges) {
                change.selectionKey.interestOps(change.ops);
            }
            this.pendingChanges.clear();

            // Wait for an event one of the registered channels
            this.selector.select();

            // Iterate over the set of keys for which events are available
            Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                SelectionKey key = selectedKeys.next();
                selectedKeys.remove();

                if (!key.isValid()) {
                    continue;
                }

                // Check what event is available and deal with it
                if (key.isAcceptable()) {
                    accept(key);
                } else if (key.isReadable()) {
                    read(key);
                } else if (key.isWritable()) {
                    write(key);
                }
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {

        // For an accept to be pending the channel must be a server selectionKey
        // channel.
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        // Accept the connection and make it non-blocking
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);

        InetSocketAddress remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
        InetSocketAddress localAddress = (InetSocketAddress) socketChannel.getLocalAddress();
        String confirmation = this.cmdProcessor.connectionAccepted(localAddress, remoteAddress);
        //send(key, confirmation.getBytes(Constants.TELNET_ENCODING));

        // Register the new SocketChannel with our Selector, indicating
        // we'd like to be notified when there's data waiting to be read
        SelectionKey registeredKey = socketChannel.register(this.selector, SelectionKey.OP_WRITE);
        queueForWrite(registeredKey, confirmation.getBytes(Constants.TELNET_ENCODING));
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        // Clear out our read buffer so it's ready for new data
        this.readBuffer.clear();

        // Attempt to read off the channel
        int numRead;
        try {
            numRead = socketChannel.read(this.readBuffer);
        } catch (IOException e) {
            InetSocketAddress remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
            this.cmdProcessor.connectionClosed(remoteAddress.getAddress());

            // The remote forcibly closed the connection, cancel
            // the selection key and close the channel.
            key.cancel();
            socketChannel.close();

            return;
        }

        if (numRead == -1) {
            InetSocketAddress remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
            this.cmdProcessor.connectionClosed(remoteAddress.getAddress());

            // Remote entity shut the selectionKey down cleanly. Do the
            // same from our end and cancel the channel.
            key.channel().close();
            key.cancel();

            return;
        }

        byte[] dataCopy = new byte[numRead];
        System.arraycopy(this.readBuffer.array(), 0, dataCopy, 0, numRead);
        //System.out.println("#tempdata:" + new String(dataCopy, Constants.TELNET_ENCODING));

        // If we have already received some data, we add this to our buffer
        if (this.pendingReads.containsKey(key)) {
            byte[] existingBytes = pendingReads.get(key);

            byte[] concatenated = new byte[existingBytes.length + dataCopy.length];
            System.arraycopy(existingBytes, 0, concatenated, 0, existingBytes.length);
            System.arraycopy(dataCopy, 0, concatenated, existingBytes.length, dataCopy.length);

            dataCopy = concatenated;
        }

        //If somebody funny sends us veeerry long requests, we just close the connection
        if (dataCopy.length > 128000) {
            InetSocketAddress remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
            this.cmdProcessor.connectionClosed(remoteAddress.getAddress());
            key.channel().close();
            key.cancel();

            this.pendingReads.remove(key);
            return;
        }

        // In case we have now finally reached all characters
        byte[] unprocessed = processReceiveBuffer(dataCopy, key);
        this.pendingReads.put(key, unprocessed);
    }

    // This is telnet specific, maybe you have to change it according to your
    private byte[] processReceiveBuffer(byte[] data, SelectionKey key) throws UnsupportedEncodingException {
        int length = data.length;
        int start = 0;
        for (int i = 1; i < length; i++) {
            if (data[i] == '\n') {
                if (i > 1 && data[i - 1] == '\r') {

                    byte[] concatenated = new byte[(i - 1) - start];
                    System.arraycopy(data, start, concatenated, 0, (i - 1) - start);

                    String tempStr = new String(concatenated, Constants.TELNET_ENCODING);
                    handleRequest(key, tempStr);

                    start = i + 1;
                }
            }
        }

        byte[] unprocessed = new byte[data.length - start];
        System.arraycopy(data, start, unprocessed, 0, unprocessed.length);

        return unprocessed;
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        InetSocketAddress remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
        List<ByteBuffer> queue = this.pendingWrites.get(key);

        // Write until there's no more data left ...
        while (!queue.isEmpty()) {
            ByteBuffer buf = queue.get(0);
            try {
                socketChannel.write(buf);
            } catch (IOException ex) {
                //There could be an IOException: Connection reset by peer
                queue.clear(); //clear the queue
                this.cmdProcessor.connectionClosed(remoteAddress.getAddress());
                return;
            }
            if (buf.remaining() > 0) {
                // ... or the selectionKey's buffer fills up
                break;
            }
            queue.remove(0);
        }

        if (queue.isEmpty()) {
            // We wrote away all data, so we're no longer interested
            // in writing on this selectionKey. Switch back to waiting for
            // data.
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    private void handleRequest(SelectionKey selectionKey, String request) {
        try {
            String res = cmdProcessor.process(request) + "\r\n";
            send(selectionKey, res.getBytes(Constants.TELNET_ENCODING));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void send(SelectionKey selectionKey, byte[] data) {
        // Indicate we want the interest ops set changed
        // If we end multiple times, since multiple commands are handled within a request, multiple OP_WRITE could end up
        // in the pending changes. But we do not care
        this.pendingChanges.add(new ChangeRequest(selectionKey, SelectionKey.OP_WRITE));

        // And queue the data we want written
        queueForWrite(selectionKey, data);

        // Finally, wake up our selecting thread so it can make the required
        // changes
        this.selector.wakeup();
    }

    private void queueForWrite(SelectionKey selectionKey, byte[] data) {
        List<ByteBuffer> queue = this.pendingWrites.get(selectionKey);
        if (queue == null) {
            queue = new ArrayList<>();
            this.pendingWrites.put(selectionKey, queue);
        }
        queue.add(ByteBuffer.wrap(data));
    }

    public void close() {
        try {
            this.serverChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

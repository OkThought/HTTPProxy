package ru.nsu.ccfit.bogush.net.http.proxy;

import ru.nsu.ccfit.bogush.net.http.HTTPMessage;
import ru.nsu.ccfit.bogush.net.http.HTTPParseException;
import ru.nsu.ccfit.bogush.net.http.HTTPRequest;
import ru.nsu.ccfit.bogush.net.http.HTTPResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

import static java.nio.channels.SelectionKey.*;

public class Proxy {
    private static final HashMap<Integer, HTTPResponse> STATUS_LINES = new HashMap<>();
    static {
        STATUS_LINES.put(400, new HTTPResponse().setStatusCode("400").setReasonPhrase("Bad Request"));
        STATUS_LINES.put(501, new HTTPResponse().setStatusCode("501").setReasonPhrase("Not Implemented"));
        STATUS_LINES.put(502, new HTTPResponse().setStatusCode("502").setReasonPhrase("Bad Gateway"));
    }
    private static final Charset US_ASCII = Charset.forName("US-ASCII");
    private static final byte[] CRLF = "\r\n".getBytes(US_ASCII);
    private static final byte CR = CRLF[0];
    private static final byte LF = CRLF[1];
    private static int port = 50505;
    private static int bufferSize = 1 << 20;
    private static int backlog = 0;
    private static InetSocketAddress localAddr;

    public static void main(String[] args) {
        localAddr = new InetSocketAddress(port);

        try {
            new Proxy().start();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private final Selector selector;

    private Proxy()
            throws IOException {
        selector = Selector.open();
    }

    private void start()
            throws IOException {
        ServerSocketChannel serverSocketChannel = selector.provider().openServerSocketChannel();
        serverSocketChannel.bind(localAddr, backlog);
        System.out.format("bound server socket on %s:%s\n", localAddr.getAddress().getHostAddress(), localAddr.getPort());
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, OP_ACCEPT, serverSocketChannel);

        System.out.println("Proxy started\n");

        while (!Thread.interrupted()) {
            loop();
        }
    }

    private void loop() throws IOException {
        while (!Thread.interrupted()) {
            int numberReady = selector.select();
            while (numberReady <= 0) {
                numberReady = selector.select();
            }

            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (!key.isValid()) {
                    iterator.remove();
                    continue;
                }

                try {
                    if (key.isAcceptable()) {
                        accept(key);
                        continue;
                    }

                    if (key.isConnectable()) {
                        connect(key);
                        continue;
                    }

                    if (key.isReadable()) {
                        int bytesRead = read(key);
                        if (bytesRead == -1) continue;
                    }

                    if (key.isWritable()) {
                        write(key);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    iterator.remove();
                }
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {
        ProxyUnit unit = new ProxyUnit(bufferSize);
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        try {
            unit.socket = serverSocketChannel.accept();
            System.out.format("%-9s %s\n", "ACCEPT", unit);
            unit.socket.configureBlocking(false);
            unit.socket.register(selector, OP_READ, unit);

        } catch (IOException e) {
            e.printStackTrace();
            if (unit.socket != null) {
                try {
                    unit.socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            // TODO: 12/10/17 correctly handle exception
            throw e;
        }
    }

    private void connect(SelectionKey key) throws IOException {
        ProxyUnit unit = (ProxyUnit) key.attachment();
        boolean connected;
        try {
            connected = unit.socket.finishConnect();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                unit.opposite.socket.close();
                unit.socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            throw e;
        }

        System.out.format("%-9s %s\n", "CONNECT", unit);
        if (connected) {
            removeOps(unit.socket.keyFor(selector), OP_CONNECT);
            addOps(unit.socket.keyFor(selector), OP_READ | OP_WRITE);
        }
    }

    private int read(SelectionKey key)
            throws IOException {
        ProxyUnit unit = (ProxyUnit) key.attachment();
        System.out.format("%-9s %s: ", "READ from", toString(unit.socket));
        return unit.read();
    }

    private int write(SelectionKey key)
            throws IOException {
        ProxyUnit unit = (ProxyUnit) key.attachment();
        System.out.format("%-9s %s: ", "WRITE to", toString(unit.socket));
        return unit.write();
    }


    private void addOps(SelectionKey key, int ops) {
        key.interestOps(key.interestOps() | ops);
    }

    private void removeOps(SelectionKey key, int ops) {
        key.interestOps(key.interestOps() & (~ops));
    }

    private static String toString(SocketAddress address) {
        if (address == null) return null;
        InetSocketAddress addr = (InetSocketAddress) address;
        String ip = addr.getAddress().getHostAddress();
        int port = addr.getPort();
        return String.format("%15s:%-5d", ip, port);
    }

    private static String toString(SelectableChannel s) {
        String str = s.toString();
        return str.substring(SocketChannel.class.getName().length());
    }

    private static String toString(SocketChannel channel) {
        String local = null;
        try {
            local = toString(channel.getLocalAddress());
        } catch (IOException ignored) { }
        String remote = null;
        try {
            remote = toString(channel.getRemoteAddress());
        } catch (IOException ignored) { }
        return String.format("[%s - %s %s]", local, remote,
                channel.isConnected() ? "connected" : channel.isConnectionPending() ? "connection pending" : "disconnected");
    }

    private class ProxyUnit {
        private ByteBuffer buf;
        private SocketChannel socket;
        private ProxyUnit opposite;
        private boolean eof = false;
        private boolean outputIsShutdown = false;
        private boolean httpMessageHeadParsed = false;

        private ProxyUnit(int bufferSize) {
            this(null, bufferSize);
        }

        private ProxyUnit(SocketChannel socket, int bufferSize) {
            this.socket = socket;
            this.buf = ByteBuffer.allocate(bufferSize);
        }

        private void connect(InetSocketAddress address)
                throws IOException {
            opposite = new ProxyUnit(socket, bufferSize);
            opposite.socket = SocketChannel.open();
            opposite.socket.configureBlocking(false);
            opposite.socket.connect(address);
            opposite.opposite = this;
            opposite.socket.register(selector, OP_CONNECT, opposite);
        }

        private int read() throws IOException {
            int bytesRead = socket.read(buf);

            System.out.format("%d bytes\n", bytesRead);

            if (bytesRead > 0) {
                if (opposite.socket.isConnected()) {
                    // opposite connected => it's a server response
                    addOps(opposite.socket.keyFor(selector), OP_WRITE);
                } else {
                    // opposite not connected => it's a client request
                    int emptyLineIndex = findEmptyLine(buf.array(), buf.position() - bytesRead, buf.position());
                    if (emptyLineIndex > 0) {
                        // request-head is between 0 and emptyLineIndex
                        // parse request

                    }
                }
            } else if (bytesRead == -1) {
                // eof
                eof = true;
                removeOps(socket.keyFor(selector), OP_READ);

                if (buf.position() == 0) {
                    opposite.shutdownOutput();
                    if (outputIsShutdown || opposite.buf.position() == 0) {
                        close();
                        opposite.close();
                    }
                }

            }

            if (!buf.hasRemaining()) {
                removeOps(socket.keyFor(selector), OP_READ);
            }
            return bytesRead;
        }

        private int findEmptyLine(byte[] array, int from, int to) {
            int cr = -3;
            int lf = -3;
            int pcr = cr-1;
            for (int i = from; i < to; i++) {
                byte b = array[i];
                if (b == CR) {
                    cr = i;
                    pcr = cr;
                } else if (b == LF) {
                    if (cr == i-1 && lf == i-2) {
                        return i - (pcr == i-3 ? 3 : 2);
                    } else if (lf == i-1) {
                        return i - (cr == i-2 ? 2 : 1);
                    }
                    lf = i;
                }
            }

            return -1;
        }



        private int write() throws IOException {
            opposite.buf.flip();

            int bytesWritten = socket.write(opposite.buf);

            if (bytesWritten > 0) {
                opposite.buf.compact();
                addOps(opposite.socket.keyFor(selector), OP_READ);
            }

            System.out.format("%d bytes\n", bytesWritten);

            if (opposite.buf.position() == 0) {
                // wrote everything from opposite.buf
                removeOps(socket.keyFor(selector), OP_WRITE);
                if (opposite.eof) {
                    shutdownOutput();
                    if (opposite.outputIsShutdown) {
                        close();
                        opposite.close();
                    }
                }
            }

            return bytesWritten;
        }

        private void shutdownOutput()
                throws IOException {
            socket.shutdownOutput();
            outputIsShutdown = true;
        }

        private void close()
                throws IOException {
            System.out.format("%-9s %s\n", "CLOSE", Proxy.toString(socket));
            socket.close();
            socket.keyFor(selector).cancel();
        }

        @Override
        public String toString() {
            return Proxy.toString(socket);
        }
    }
}

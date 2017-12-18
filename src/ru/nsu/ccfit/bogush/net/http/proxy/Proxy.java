package ru.nsu.ccfit.bogush.net.http.proxy;

import ru.nsu.ccfit.bogush.net.http.HTTPMessage;
import ru.nsu.ccfit.bogush.net.http.HTTPParseException;
import ru.nsu.ccfit.bogush.net.http.HTTPRequest;
import ru.nsu.ccfit.bogush.net.http.HTTPResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
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

    private void loop()
            throws IOException {
        int selected;
        do {
            selected = selector.select();
            System.err.println("Selector returned: " + selected);
        } while (selected <= 0);

        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();

            try {
                if (!key.isValid()) {
                    continue;
                }

                if (key.isAcceptable()) {
                    accept(key);
                    continue;
                }

                if (key.isConnectable()) {
                    connect(key);
                    continue;
                }

                if (key.isReadable()) {
                    read(key);
                }

                if (key.isWritable()) {
                    write(key);
                }
            } finally {
                iterator.remove();
            }
        }
    }

    private void accept(SelectionKey key)
            throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.attachment();
        ProxyUnit unit = new ProxyUnit();
        unit.local = serverSocketChannel.accept();
        if (unit.local == null) return;
        unit.local.configureBlocking(false);
        unit.local.register(selector, OP_READ, unit);
    }

    private static ProxyUnit attachmentAsUnit(SelectionKey key) {
        return (ProxyUnit) key.attachment();
    }

    private void connect(SelectionKey key)
            throws IOException {
        ProxyUnit unit = attachmentAsUnit(key);
        unit.finishConnect();
    }

    private void read(SelectionKey key)
            throws IOException {
        ProxyUnit unit = attachmentAsUnit(key);
        boolean straight = key.channel() == unit.local;
        unit.read(straight);
    }

    private void write(SelectionKey key)
            throws IOException {
        ProxyUnit unit = attachmentAsUnit(key);
        boolean straight = key.channel() == unit.remote;
        unit.write(straight);
    }

    private void addOps(SelectionKey key, int ops) {
        key.interestOps(key.interestOps() | ops);
    }

    private void removeOps(SelectionKey key, int ops) {
        key.interestOps(key.interestOps() & (~ops));
    }

    private class ProxyUnit {
        private SocketChannel local;
        private SocketChannel remote;
        private SocketChannel in;
        private SocketChannel out;
        private HTTPRequest request = new HTTPRequest(bufferSize);
        private HTTPRequest response = new HTTPRequest(bufferSize);

        private ByteBuffer buf;
        private boolean connected = false;
        private boolean connecting = false;

        private void connect()
                throws IOException {
            if (!Objects.equals(request.getProtocol(), "http:")) {
                local.shutdownInput();
//                inverseBuf.put();
            }
            String host = request.getHost();
            String portString = request.getPort();
            if (portString == null) {
                port = 80;
            } else {
                port = Integer.parseInt(portString);
            }

            InetSocketAddress address = new InetSocketAddress(host, port);
            remote = local.provider().openSocketChannel();
            remote.configureBlocking(false);
            remote.register(selector, OP_CONNECT, this);
            connecting = true;
            connected = remote.connect(address);
        }

        private void finishConnect()
                throws IOException {
            connected = remote.finishConnect();
        }

        private void configureDirection(boolean straight) {
            in = straight ? local : remote;
            out = straight ? remote : local;
            buf = straight ? request.buffer() : response.buffer();
        }

        private void read(boolean straight)
                throws IOException {
            configureDirection(straight);
            buf.mark();
            int bytesRead = in.read(buf);
            if (!buf.hasRemaining()) {
                removeOps(in.keyFor(selector), OP_READ);
            }
            if (bytesRead > 0) {
                if (straight) {
                    if (!request.isHeadReady()) {
                        try {
                            int parseResult = request.parse();
                            if (!connecting && (parseResult & HTTPRequest.URI) == HTTPRequest.URI) {
                                connect();
                            }
                            if ((parseResult & HTTPRequest.FULL_REQUEST_HEAD) == HTTPRequest.FULL_REQUEST_HEAD) {
                                request.setProtocol(null);
                                request.setPort(null);
                                request.setHost(null);
                                request.setVersion("HTTP/1.0");
                                request.setField("Connection", "close");
                                request.build();
                            }
                        } catch (HTTPParseException e) {
                            e.printStackTrace();
                            // TODO send Bad Request
                        }
                    }
                } else {
                    if (!response.isHeadReady()) {
                        try {
                            int parseResult = response.parse();
                            if ((parseResult & HTTPMessage.FULL_RESPONSE_HEAD) == HTTPRequest.FULL_RESPONSE_HEAD) {
                                response.setVersion("HTTP/1.0");
                                request.setField("Connection", "close");
                                response.build();
                            }
                        } catch (HTTPParseException e) {
                            e.printStackTrace();
                            // TODO send Bad Request
                        }
                    }
                }
            } else if (bytesRead == -1) {
                // EOF from in
                if (out.isOpen()) {
                    out.shutdownInput();
                }
                in.keyFor(selector).cancel();
                in.close();
            } else {
                System.err.format("read %d bytes - WTF?!\n\n", bytesRead);
            }
        }

        private void write(boolean straight)
                throws IOException {
            configureDirection(straight);
            buf.flip();
            int bytesWritten = out.write(buf);
            if (bytesWritten > 0) {
                buf.compact();
            }
        }
    }
}

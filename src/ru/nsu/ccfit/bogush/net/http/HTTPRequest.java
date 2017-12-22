package ru.nsu.ccfit.bogush.net.http;

public class HTTPRequest extends HTTPMessage {
    private String method;
    private String protocol;
    private String host;
    private int port = -1;
    private String path;
    private String query;


    public String getRequestLine() {
        return method + SP + getURI() + SP + version;
    }

    public String getMethod() {
        return method;
    }

    public HTTPRequest setMethod(String method) {
        this.method = method;
        return this;
    }

    public String getURI() {
        return (protocol == null || protocol.isEmpty() ? "" : protocol + "://") +
                host + (port == -1 ? "" : ":" + port) + path + query;
    }

    public String getProtocol() {
        return protocol;
    }

    public HTTPRequest setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public String getHost() {
        return host;
    }

    public HTTPRequest setHost(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public HTTPRequest setPort(int port) {
        this.port = port;
        return this;
    }

    public String getPath() {
        return path;
    }

    public HTTPRequest setPath(String path) {
        this.path = path;
        return this;
    }

    public String getQuery() {
        return query;
    }

    public HTTPRequest setQuery(String query) {
        this.query = query;
        return this;
    }

    @Override
    public String toString() {
        return  getRequestLine() + CRLF +
                getFieldsString() + CRLF + CRLF;
    }
}

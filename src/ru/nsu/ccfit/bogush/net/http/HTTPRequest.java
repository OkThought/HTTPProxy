package ru.nsu.ccfit.bogush.net.http;

/**
 * Java-bean object for storing HTTPRequest head information. <br>
 *
 * HTTP Response scheme: <br>
 * <code>
 * METHOD SP URI SP VERSION CR LF <br>
 * <br>
 * METHOD = GET | POST | HEAD <br>
 * URI = [ protocol "://" ] [ host [ ":" port ] ] path query <br>
 * path = "/" [ path-element1 "/" [ path-element2 "/" ] ] <br>
 * VERSION = HTTP / digit "." digit <br>
 * </code>
 */
public class HTTPRequest extends HTTPMessage {
    private static final int UNSPECIFIED_PORT = -1;
    private String method;
    private String protocol;
    private String host;
    private int port = UNSPECIFIED_PORT;
    private String path;
    private String query;


    public String getRequestLine() {
        return method + SP + getURI() + SP + version;
    }

    public String getURI() {
        return (protocolSpecified() ? protocol + "://" : "") +
                (hostSpecified() ? host + (portSpecified() ? ":" + port : "") : "") + path + query;
    }

    public String getMethod() {
        return method;
    }

    public boolean methodSpecified() {
        return method != null && !method.isEmpty();
    }

    public HTTPRequest setMethod(String method) {
        this.method = method;
        return this;
    }

    public String getProtocol() {
        return protocol;
    }

    public boolean protocolSpecified() {
        return protocol != null && !protocol.isEmpty();
    }

    public HTTPRequest setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public HTTPRequest resetProtocol() {
        protocol = null;
        return this;
    }

    public String getHost() {
        return host;
    }

    public boolean hostSpecified() {
        return host != null && !host.isEmpty();
    }

    public HTTPRequest setHost(String host) {
        this.host = host;
        return this;
    }

    public HTTPRequest resetHost() {
        host = null;
        return this;
    }

    public int getPort() {
        return port;
    }

    public boolean portSpecified() {
        return port == UNSPECIFIED_PORT;
    }

    public HTTPRequest setPort(int port) {
        this.port = port;
        return this;
    }

    public HTTPRequest resetPort() {
        return setPort(UNSPECIFIED_PORT);
    }

    public String getPath() {
        return path;
    }

    public boolean pathSpecified() {
        return path != null && !path.isEmpty();
    }

    public HTTPRequest setPath(String path) {
        this.path = path;
        return this;
    }

    public String getQuery() {
        return query;
    }

    public boolean querySpecified() {
        return query != null && !query.isEmpty();
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

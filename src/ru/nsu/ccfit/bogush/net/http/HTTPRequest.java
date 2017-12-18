package ru.nsu.ccfit.bogush.net.http;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTTPRequest extends HTTPMessage {
    protected static final Pattern URI_PATTERN = Pattern.compile(
            "(?:(?<protocol>[a-zA-Z]+)://)?(?:(?<host>[^/:]+)(?::(?<port>\\d{1,5}))?)?(?<path>\\S*/)?(?<query>\\S+)");

    private String method;
    private String protocol;
    private String host;
    private String port;
    private String path;
    private String query;

    public HTTPRequest() {}

    public HTTPRequest(int bufferSize) {
        super(bufferSize);
    }

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
                host + (port == null ? "" : ":" + port) + path + query;
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

    public String getPort() {
        return port;
    }

    public HTTPRequest setPort(int port) {
        this.port = String.valueOf(port);
        return this;
    }

    public HTTPRequest setPort(String port) {
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
    public byte[] head() {
        return toString().getBytes(ASCII);
    }

    @Override
    public void reset() {
        super.reset();
        method = null;
        protocol = null;
        host = null;
        port = null;
        path = null;
        query = null;
    }

    public int parse()
            throws HTTPParseException, IOException {
        int result = NONE;

        initReader();

        if (method == null) {
            result |= parseMethod();
        }

        if (query == null) {
            result |= parseURI();
        }

        if (version == null) {
            result |= parseVersion();
        }

        if (body == -1) {
            result |= parseHeaders();
        }

        headReady = true;

        return result;
    }

    private int parseMethod()
            throws HTTPParseException, IOException {
        int r;
        while ((r = reader.read()) != -1) {
            ++pos;
            char c = (char) r;
            if (c == SP) {
                method = substring(mark, pos);
                mark = pos + 1;
                return METHOD;
            } else if (c == CR || c == LF) {
                throw new HTTPParseException("unexpected CR or LF found", pos);
            }
        }

        return NONE;
    }

    private int parseURI()
            throws HTTPParseException, IOException {
        int r;
        while ((r = reader.read()) != -1) {
            ++pos;
            char c = (char) r;
            if (c == SP) {
                CharSequence uri = substring(mark, pos);
                parseURI(uri);
                if (query == null) {
                    throw new HTTPParseException("failed to parse query", pos);
                }
                mark = pos + 1;
                return URI;
            } else if (c == CR || c == LF) {
                throw new HTTPParseException("unexpected CR or LF found", pos);
            }
        }

        return NONE;
    }

    private void parseURI(CharSequence uri) {
        Matcher matcher = URI_PATTERN.matcher(uri);
        protocol = matcher.group("protocol");
        host = matcher.group("host");
        port = matcher.group("port");
        path = matcher.group("path");
        query = matcher.group("query");

    }

    @Override
    public String toString() {
        return  getRequestLine() + CRLF +
                getFieldsString() + CRLF + CRLF;
    }
}

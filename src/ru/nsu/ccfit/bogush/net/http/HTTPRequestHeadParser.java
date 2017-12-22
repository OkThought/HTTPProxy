package ru.nsu.ccfit.bogush.net.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.nsu.ccfit.bogush.net.http.Constants.*;

public class HTTPRequestHeadParser extends HTTPMessageHeadParser {
    private static final String PROTOCOL_PATTERN_STRING = "(?:(?<protocol>[a-zA-Z]+)://)";
    private static final String HOST_PATTERN_STRING = "(?:(?<host>[^/:]+)(?::(?<port>\\d{1,5}))?)";
    private static final String PATH_PATTERN_STRING = "(?<path>\\S*/)";
    private static final String QUERY_PATTERN_STRING = "(?<query>\\S+)";
    private static final String URI_PATTERN_STRING = PROTOCOL_PATTERN_STRING + "?" + HOST_PATTERN_STRING + "?" +
            PATH_PATTERN_STRING + "?" + QUERY_PATTERN_STRING;
    private static final String METHOD_PATTERN_STRING = "(?<method>GET|POST|HEAD)";
    private static final String REQUEST_LINE_PATTERN_STRING = METHOD_PATTERN_STRING + SP + URI_PATTERN_STRING + SP +
            VERSION_PATTERN_STRING + "?" + CR + "?" + LF;
    private static final Pattern REQUEST_LINE_PATTERN = Pattern.compile(REQUEST_LINE_PATTERN_STRING);

    private HTTPRequest request;

    public HTTPRequestHeadParser(ByteBuffer byteBuffer) {
        this(byteBuffer, new HTTPRequest());
    }

    public HTTPRequestHeadParser(ByteBuffer byteBuffer, HTTPRequest request) {
        super(byteBuffer, request);
        this.request = request;
    }

    public HTTPRequestHeadParser(byte[] array) {
        this(array, new HTTPRequest());
    }

    public HTTPRequestHeadParser(byte[] array, HTTPRequest request) {
        this(array, 0, array.length, request);
    }

    public HTTPRequestHeadParser(byte[] array, int offset, int length) {
        this(array, offset, length, new HTTPRequest());
    }

    public HTTPRequestHeadParser(byte[] array, int offset, int length, HTTPRequest request) {
        super(array, offset, length, request);
    }

    public HTTPRequest parse()
            throws IOException, HTTPParseException {
        pos = 0;
        parseRequestLine();
        parseFields();
        return request;
    }

    private void parseRequestLine()
            throws HTTPParseException {
        int requestLineEnd = findLineEnd(bytes, pos, length);
        if (requestLineEnd == -1) {
            throw new HTTPParseException("CRLF nor LF not found", length);
        }

        parseRequestLine(substring(pos, requestLineEnd));
        pos = requestLineEnd + 1;
    }

    private void parseRequestLine(String s)
            throws HTTPParseException {
        Matcher matcher = REQUEST_LINE_PATTERN.matcher(s);

        String method = matcher.group("method");
        if (method != null) {
            request.setMethod(method);
            pos += method.length() + 1;
        } else {
            throw new HTTPParseException("Couldn't parse method", pos);
        }

        String protocol = matcher.group("protocol");
        if (protocol != null) {
            request.setProtocol(protocol);
            pos += protocol.length();
        } else {
            request.setProtocol(DEFAULT_PROTOCOL);
        }

        String host = matcher.group("host");
        if (host != null) {
            request.setHost(host);
            pos += host.length();
        } else {
            request.setHost(DEFAULT_HOST);
        }

        String port = matcher.group("port");
        if (port != null) {
            try {
                request.setPort(Integer.parseInt(port));
            } catch (NumberFormatException e) {
                throw new HTTPParseException(e.getMessage(), pos);
            }
            pos += port.length();
        } else {
            request.setPort(DEFAULT_PORT);
        }

        String path = matcher.group("path");
        if (path != null) {
            request.setPath(path);
            pos += path.length();
        } else {
            request.setPath(DEFAULT_PATH);
        }

        String query = matcher.group("query");
        if (query != null) {
            request.setQuery(query);
            pos += query.length() + 1;
        } else {
            throw new HTTPParseException("Couldn't parse query", pos);
        }

        String version = matcher.group("version");
        if (version != null) {
            request.setVersion(version);
            pos += version.length();
        } else {
            request.setVersion(DEFAULT_VERSION);
        }
    }
}

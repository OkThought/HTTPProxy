package ru.nsu.ccfit.bogush.net.http;

import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTTPResponseHeadParser extends HTTPMessageHeadParser {
    private static final String STATUS_CODE_PATTERN_STRING = "(?<status>\\d{3})";
    private static final String REASON_PHRASE_PATTERN_STRING = "(?<reason>[\\w" + SP + HT + "]*)";
    private static final String STATUS_LINE_PATTERN_STRING = VERSION_PATTERN_STRING + SP + STATUS_CODE_PATTERN_STRING +
            SP + REASON_PHRASE_PATTERN_STRING + CR + "?" + LF;
    private static final Pattern STATUS_LINE_PATTERN = Pattern.compile(STATUS_LINE_PATTERN_STRING);
    private static final String DEFAULT_REASON_PHRASE = "";

    static {
        Pattern.compile("(?<reason>[\\w]*)");}

    private HTTPResponse response;

    public HTTPResponseHeadParser(ByteBuffer byteBuffer) {
        this(byteBuffer, new HTTPResponse());
    }

    public HTTPResponseHeadParser(ByteBuffer byteBuffer, HTTPResponse response) {
        super(byteBuffer, response);
        this.response = response;
    }

    public HTTPResponseHeadParser(byte[] array) {
        this(array, new HTTPResponse());
    }

    public HTTPResponseHeadParser(byte[] array, HTTPResponse response) {
        super(array, response);
        this.response = response;
    }

    public HTTPResponseHeadParser(byte[] array, int offset, int length) {
        this(array, offset, length, new HTTPResponse());
    }

    public HTTPResponseHeadParser(byte[] array, int offset, int length, HTTPResponse response) {
        super(array, offset, length, response);
        this.response = response;
    }

    public HTTPResponse parse()
            throws HTTPParseException {
        pos = 0;
        parseStatusLine();
        parseFields();
        return response;
    }

    private void parseStatusLine()
            throws HTTPParseException {
        int requestLineEnd = findLineEnd(bytes, pos, length);
        if (requestLineEnd == -1) {
            throw new HTTPParseException("CRLF nor LF not found", length);
        }

        parseStatusLine(substring(pos, requestLineEnd));
        pos = requestLineEnd + 1;
    }

    private void parseStatusLine(String s)
            throws HTTPParseException {
        Matcher matcher = STATUS_LINE_PATTERN.matcher(s);

        String version = matcher.group("version");
        if (version != null) {
            response.setVersion(version);
            pos += version.length();
        } else {
            throw new HTTPParseException("Couldn't parse version", pos);
        }

        String status = matcher.group("status");
        if (status != null) {
            response.setStatusCode(status);
            pos += status.length();
        } else {
            throw new HTTPParseException("Couldn't status code", pos);
        }

        String reason = matcher.group("reason");
        if (reason != null) {
            response.setReasonPhrase(reason);
            pos += reason.length();
        } else {
            response.setReasonPhrase(DEFAULT_REASON_PHRASE);
        }
    }
}

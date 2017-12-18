package ru.nsu.ccfit.bogush.net.http;

import java.io.IOException;

public class HTTPResponse extends HTTPMessage {
    private String statusCode;
    private String reasonPhrase;
    private boolean headReady = false;

    public HTTPResponse() {}

    public HTTPResponse(int bufferSize) {
        super(bufferSize);
    }

    public String getStatusLine() {
        return version + SP + statusCode + SP + reasonPhrase;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public HTTPResponse setStatusCode(String statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public HTTPResponse setReasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
        return this;
    }

    @Override
    public byte[] head() {
        return toString().getBytes(ASCII);
    }

    @Override
    public void reset() {
        super.reset();
        statusCode = null;
        reasonPhrase = null;
    }

    public int parse()
            throws HTTPParseException, IOException {
        int result = NONE;

        initReader();

        if (version == null) {
            result |= parseVersion();
        }

        if (statusCode == null) {
            result |= parseStatusCode();
        }

        if (reasonPhrase == null) {
            result |= parseReasonPhrase();
        }

        if (body == -1) {
            result |= parseHeaders();
        }

        headReady = true;

        return result;
    }

    private int parseStatusCode()
            throws HTTPParseException, IOException {
        int r;
        while ((r = reader.read()) != -1) {
            ++pos;
            char c = (char) r;
            if (c == SP) {
                statusCode = substring(mark, pos);
                mark = pos + 1;
                return STATUS_CODE;
            } else if (c == CR || c == LF) {
                throw new HTTPParseException("unexpected CR or LF found", pos);
            }
        }

        return NONE;
    }

    private int parseReasonPhrase()
            throws HTTPParseException, IOException {
        int r;
        while ((r = reader.read()) != -1) {
            ++pos;
            char c = (char) r;
            if (c == LF) {
                reasonPhrase = substring(mark, pos).trim();
                mark = pos + 1;
                return REASON_PHRASE;
            }
        }

        return NONE;
    }

    @Override
    public String toString() {
        return  getStatusLine() + CRLF +
                getFieldsString() + CRLF + CRLF;
    }
}

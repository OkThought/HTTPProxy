package ru.nsu.ccfit.bogush.net.http;

import ru.nsu.ccfit.bogush.net.http.build.HTTPMessageBuilder;
import ru.nsu.ccfit.bogush.net.http.build.HTTPResponseBuilder;

/**
 * Java-bean object for storing HTTPResponse head information. <br>
 *
 * HTTP Request scheme: <br>
 * <code>
 * VERSION SP STATUS-CODE SP REASON-PHRASE CR LF<br>
 * <br>
 * VERSION = HTTP "/" digit "." digit <br>
 * STATUS-CODE = 3 * digit <br>
 * REASON-PHRASE = TEXT <br>
 * </code>
 */
public class HTTPResponse extends HTTPMessage {
    private String statusCode;
    private String reasonPhrase;

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
    public HTTPMessageBuilder createBuilder() {
        return new HTTPResponseBuilder(this);
    }

    @Override
    public String toString() {
        return  getStatusLine() + CRLF +
                getFieldsString() + CRLF + CRLF;
    }
}

package ru.nsu.ccfit.bogush.net.http;

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
    public String toString() {
        return  getStatusLine() + CRLF +
                getFieldsString() + CRLF + CRLF;
    }
}

package ru.nsu.ccfit.bogush.net.http.build;

import ru.nsu.ccfit.bogush.net.http.HTTPRequest;

import java.nio.ByteBuffer;

import static ru.nsu.ccfit.bogush.net.http.Constants.*;

public class HTTPRequestBuilder extends HTTPMessageBuilder {
    private HTTPRequest request;

    public HTTPRequestBuilder(HTTPRequest request) {
        super(request);
        this.request = request;
    }

    @Override
    public int write(ByteBuffer buffer) {
        int p = buffer.position();

        buffer.put(request.getRequestLine().getBytes(US_ASCII)).put((byte) CR).put((byte) LF);
        buffer.put(request.getFieldsString().getBytes(US_ASCII)).put((byte) CR).put((byte) LF).put((byte) CR).put((byte) LF);

        return buffer.position() - p;
    }
}

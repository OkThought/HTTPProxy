package ru.nsu.ccfit.bogush.net.http.build;

import ru.nsu.ccfit.bogush.net.http.HTTPResponse;

import java.nio.ByteBuffer;

import static ru.nsu.ccfit.bogush.net.http.Constants.CR;
import static ru.nsu.ccfit.bogush.net.http.Constants.LF;
import static ru.nsu.ccfit.bogush.net.http.Constants.US_ASCII;

public class HTTPResponseBuilder extends HTTPMessageBuilder {
    private HTTPResponse response;

    public HTTPResponseBuilder(HTTPResponse response) {
        super(response);
        this.response = response;
    }

    @Override
    public int write(ByteBuffer buffer) {
        int p = buffer.position();

        buffer.put(response.getStatusLine().getBytes(US_ASCII)).put((byte) CR).put((byte) LF);
        buffer.put(response.getFieldsString().getBytes(US_ASCII)).put((byte) CR).put((byte) LF).put((byte) CR).put((byte) LF);

        return buffer.position() - p;
    }
}

package ru.nsu.ccfit.bogush.net.http.build;

import ru.nsu.ccfit.bogush.net.http.HTTPMessage;

import java.nio.ByteBuffer;

public abstract class HTTPMessageBuilder {
    private HTTPMessage message;

    protected HTTPMessageBuilder(HTTPMessage message) {
        this.message = message;
    }

    public abstract int write(ByteBuffer buffer);
}

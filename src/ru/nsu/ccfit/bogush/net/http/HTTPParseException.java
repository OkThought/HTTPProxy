package ru.nsu.ccfit.bogush.net.http;

import java.text.ParseException;

public class HTTPParseException extends ParseException {
    public HTTPParseException(String s, int errorOffset) {
        super(s, errorOffset);
    }
}

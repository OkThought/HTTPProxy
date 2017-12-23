package ru.nsu.ccfit.bogush.net.http.parse;

import java.text.ParseException;

public class HTTPParseException extends ParseException {
    public HTTPParseException(String s, int errorOffset) {
        super(s, errorOffset);
    }
}

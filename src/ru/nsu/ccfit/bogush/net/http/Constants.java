package ru.nsu.ccfit.bogush.net.http;

import java.nio.charset.Charset;

public abstract class Constants {
    public static final String DEFAULT_PROTOCOL = "http";
    public static final String DEFAULT_HOST = "";
    public static final int DEFAULT_PORT = 80;
    public static final String DEFAULT_PATH = "";
    public static final String DEFAULT_REASON_PHRASE = "";
    public static final String DEFAULT_VERSION = "HTTP/1.0";

    protected static final Charset US_ASCII = Charset.forName("US-ASCII");
    protected static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
    protected static final Charset UTF_8 = Charset.forName("UTF-8");

    protected static final char SP = ' ';
    protected static final char HT = '\t';
    protected static final char LF = '\n';
    protected static final char CR = '\r';
    protected static final String CRLF = "" + CR + LF;
}

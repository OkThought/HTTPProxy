package ru.nsu.ccfit.bogush.net.http;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class HTTPMessageHeadParser {
    protected static final Charset US_ASCII = Charset.forName("US-ASCII");
    protected static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
    protected static final Charset UTF_8 = Charset.forName("UTF-8");

    protected static final char SP = ' ';
    protected static final char HT = '\t';
    protected static final char LF = '\n';
    protected static final char CR = '\r';
    protected static final String CRLF = "" + CR + LF;

    protected static final String VERSION_PATTERN_STRING = "(?<version>HTTP/\\d.\\d)";
    private static final boolean ALLOW_MULTILINE_FIELDS = false;
    private static final String FIELD_DELIMITER_PATTERN_STRING = CR + "?" + LF + (ALLOW_MULTILINE_FIELDS ? "(?![" + SP + HT + "])" : "");
    private static final String FIELD_PATTERN_STRING = "(?<name>.+):(?<value>.*)";
    private static final Pattern FIELD_PATTERN = Pattern.compile(FIELD_PATTERN_STRING);
    protected static final String DEFAULT_VERSION = "HTTP/1.0";

    protected static int findLineEnd(byte[] array, int from, int to) {
        for (int i = from; i < to; i++) {
            byte b = array[i];
            if (b == LF) {
                return i;
            }
        }
        return -1;
    }

    private HTTPMessage message;
    protected byte[] bytes;
    protected int offset;
    protected int length;

    protected int pos = -1;

    protected HTTPMessageHeadParser(ByteBuffer byteBuffer, HTTPMessage message) {
        this(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit(), message);
    }

    protected HTTPMessageHeadParser(byte[] array, HTTPMessage message) {
        this(array, 0, array.length, message);
    }

    protected HTTPMessageHeadParser(byte[] array, int offset, int length, HTTPMessage message) {
        this.bytes = array;
        this.message = message;
        this.offset = offset;
        this.length = length;
    }

    protected String substring(int from, int to) {
        return substring(from, to, US_ASCII);
    }

    protected String substring(int from, int to, Charset charset) {
        return new String(bytes, from, to-from, charset);
    }

    protected void parseFields()
            throws HTTPParseException {
        parseFields(substring(pos, length, ISO_8859_1));
    }

    protected void parseFields(String s)
            throws HTTPParseException {
        for (String field : s.split(FIELD_DELIMITER_PATTERN_STRING)) {
            parseField(field);
        }
    }

    protected void parseField(String field)
            throws HTTPParseException {
        Matcher matcher = FIELD_PATTERN.matcher(field);

        String name = matcher.group("name");
        if (name == null) {
            throw new HTTPParseException("Couldn't parse field name", pos);
        }

        String value = matcher.group("value");
        if (value == null) {
            throw new HTTPParseException("Couldn't parse field value", pos);
        }

        message.setField(name, value);
        pos += field.length() + 2;
    }
}

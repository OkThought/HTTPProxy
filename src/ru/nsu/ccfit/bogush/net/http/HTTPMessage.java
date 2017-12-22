package ru.nsu.ccfit.bogush.net.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class HTTPMessage {
    public static final int NONE                = 0;
    public static final int VERSION             = 0b1;
    public static final int HEADERS             = 0b10;
    public static final int EMPTY_LINE          = 0b100;

    public static final int METHOD              = 0b1000;
    public static final int URI                 = 0b10000;

    public static final int REQUEST_LINE        = METHOD | URI | VERSION;
    public static final int FULL_REQUEST_HEAD   = REQUEST_LINE | HEADERS | EMPTY_LINE;

    public static final int STATUS_CODE         = 0b100000;
    public static final int REASON_PHRASE       = 0b1000000;
    public static final int STATUS_LINE         = VERSION | STATUS_CODE | REASON_PHRASE;
    public static final int FULL_RESPONSE_HEAD  = STATUS_LINE | HEADERS | EMPTY_LINE;

    protected static final Charset US_ASCII = Charset.forName("US-ASCII");
    protected static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
    protected static final Charset UTF_8 = Charset.forName("UTF-8");

    protected static final char SP = ' ';
    protected static final char HT = '\t';
    protected static final char LF = '\n';
    protected static final char CR = '\r';
    protected static final String CRLF = "" + CR + LF;
    protected static final boolean ALLOW_MULTILINE_FIELDS = false;
    protected static final Pattern FIELD_DELIMITER_PATTERN = Pattern.compile(
            CR + "?" + LF + (ALLOW_MULTILINE_FIELDS ? "(?![" + SP + HT + "])" : "")
    );
    protected static final Pattern FIELD_PATTERN = Pattern.compile("(?<header>.+):(?<value>.*)");

    protected ByteBuffer byteBuffer;
    protected CharBuffer chars;
//    protected CharBuffer octets;
    protected HashMap<String, String> fields;
    protected String version;
    protected int mark = 0;
    protected int cr = -3;
    protected int lf = -3;
    protected int body = -1;
    protected int pos = -1;
    protected boolean headReady = false;

    protected HTTPMessage() {
        byteBuffer = null;
        chars = null;
    }

    protected HTTPMessage(int bufferSize) {
        this(ByteBuffer.allocate(bufferSize));
    }

    protected HTTPMessage(ByteBuffer buffer) {
        byteBuffer = buffer;
        byteBuffer.mark();
    }

    public ByteBuffer buffer() {
        return byteBuffer;
    }

    public String getFieldsString() {
        if (fields == null) return "";
        return fields.entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(CRLF));
    }

    public String getVersion() {
        return version;
    }

    public HTTPMessage setVersion(String version) {
        this.version = version;
        return this;
    }

    public HashMap<String, String> getFields() {
        return fields;
    }

    public Collection<String> getFieldValues() {
        if (fields == null) return null;
        return fields.values();
    }

    public String getFieldValue(String header) {
        if (fields == null) return null;
        return fields.get(header);
    }

    public HTTPMessage setField(String name, String value) {
        if (fields == null) {
            fields = new HashMap<>();
        }
        fields.put(name, value);
        return this;
    }

    public String removeField(String name) {
        if (fields == null) return null;
        return fields.remove(name);
    }

    public boolean removeField(String name, String value) {
        return fields != null && fields.remove(name, value);
    }

    public Set<String> getHeaders() {
        if (fields == null) return null;
        return fields.keySet();
    }

    public void reset() {
        mark = 0;
        cr = -3;
        lf = -3;
        body = -1;
        version = null;
        fields = null;
        byteBuffer.reset();
        chars.reset();
    }

    public boolean isHeadReady() {
        return headReady;
    }

    public abstract byte[] head();

    public ByteBuffer body() {
        if (body == -1 || byteBuffer == null) {
            return ByteBuffer.allocate(0);
        }
        byteBuffer.mark();
        byteBuffer.position(body);
        ByteBuffer buf = byteBuffer.slice();
        byteBuffer.reset();
        return buf;
    }

    public HTTPMessage build() {
        byte[] head = head();
        ByteBuffer body = body();
        ByteBuffer buf = ByteBuffer.allocate(head.length + body.capacity());
        buf.put(head).putChar(CR).putChar(LF).putChar(CR).putChar(LF).put(body);
        byteBuffer = buf;
        return this;
    }

    protected int parseVersion()
            throws HTTPParseException, IOException {
        while (chars.hasRemaining()) {
            ++pos;
            char c = chars.get();
            if (c == LF) {
                if (pos - mark <= 2) {
                    throw new HTTPParseException("unexpected LF found", pos);
                }
                version = substring(mark, pos).trim();
                mark = pos + 1;
                lf = pos;
                return VERSION;
            }
        }

        return NONE;
    }

    protected int parseHeaders()
            throws HTTPParseException, IOException {
        while (chars.hasRemaining()) {
            ++pos;
            char c = chars.get();
            if (c == LF) {
                if (!ALLOW_MULTILINE_FIELDS) {
                    parseHeader(substring(mark, pos, UTF_8).trim());
                }

                if (lf == pos - 1 ||
                        cr == pos - 1 && lf == pos - 2) {
                    body = pos + 1;
                    if (ALLOW_MULTILINE_FIELDS) {
                        parseHeaders(substring(mark, pos, UTF_8).trim());
                    }
                    return HEADERS | EMPTY_LINE;
                }
                lf = pos;
            } else if (c == CR) {
                cr = pos;
            }
        }

        return NONE;
    }

    protected void decodeChars() {
        int p = byteBuffer.position();
        int l = byteBuffer.limit();
        byteBuffer.reset().limit(p);
        chars = UTF_8.decode(byteBuffer);
        byteBuffer.reset().limit(p);
        System.out.println("decoded: " + chars.toString());
//        octets = ISO_8859_1.decode(byteBuffer);
        byteBuffer.mark();
        byteBuffer.position(p).limit(l);
    }

    protected String substring(int from, int to) {
        return substring(from, to, US_ASCII);
    }

    protected String substring(int from, int to, Charset charset) {
        return new String(byteBuffer.array(), from, to-from, charset);
    }

    protected void parseHeader(String headerLine) {
        Matcher matcher = FIELD_PATTERN.matcher(headerLine);
        String header = matcher.group("header");
        String value = matcher.group("value");
        fields.put(header, value);
    }

    protected void parseHeaders(String headersString)
            throws HTTPParseException {
        fields = new HashMap<>();
        String[] headerLines = FIELD_DELIMITER_PATTERN.split(headersString);
        for (String headerLine: headerLines) {
            Matcher matcher = FIELD_PATTERN.matcher(headerLine);
            fields.put(matcher.group("header"), matcher.group("value"));
        }
    }
}

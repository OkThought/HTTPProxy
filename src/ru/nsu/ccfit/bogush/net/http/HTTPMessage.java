package ru.nsu.ccfit.bogush.net.http;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class HTTPMessage {
    protected static final char SP = ' ';
    protected static final char HT = '\t';
    protected static final char LF = '\n';
    protected static final char CR = '\r';
    protected static final String CRLF = "" + CR + LF;

    protected HashMap<String, String> fields = new HashMap<>();
    protected String version;

    protected String getFieldsString() {
        if (fields == null) return "";
        return fields.entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(CRLF));
    }

    public String getVersion() {
        return version;
    }

    public boolean versionSpecified() {
        return version != null && !version.isEmpty();
    }

    public HTTPMessage setVersion(String version) {
        this.version = version;
        return this;
    }

    public HTTPMessage resetVersion() {
        return setVersion(null);
    }

    public HashMap<String, String> getFields() {
        return fields;
    }

    public boolean fieldsSpecified() {
        return fields != null && !fields.isEmpty();
    }

    public Collection<String> getFieldValues() {
        if (fields == null) return null;
        return fields.values();
    }

    public String getFieldValue(String name) {
        if (fields == null) return null;
        return fields.get(name);
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

    public Set<String> getFieldNames() {
        if (fields == null) return null;
        return fields.keySet();
    }
}

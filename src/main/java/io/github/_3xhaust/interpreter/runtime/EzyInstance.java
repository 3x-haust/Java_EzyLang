package io.github._3xhaust.interpreter.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class EzyInstance {
    private final EzyClass ezyClass;
    private final Map<String, Object> fields = new HashMap<>();

    public EzyInstance(EzyClass ezyClass) {
        this.ezyClass = ezyClass;
    }

    public EzyClass getEzyClass() { return ezyClass; }

    public Object getField(String name) { return fields.get(name); }

    public void setField(String name, Object value) { fields.put(name, value); }

    public boolean hasField(String name) { return fields.containsKey(name); }

    public Map<String, Object> getFields() { return fields; }

    public boolean isInstanceOf(EzyClass other) {
        return ezyClass.isSubclassOf(other);
    }

    public boolean isInstanceOfInterface(String interfaceName) {
        return ezyClass.implementsInterface(interfaceName);
    }

    @Override
    public String toString() {
        if (ezyClass.hasDecorator("data") || ezyClass.hasDecorator("toString")) {
            String fieldsStr = fields.entrySet().stream()
                    .map(e -> e.getKey() + "=" + formatValue(e.getValue()))
                    .collect(Collectors.joining(", "));
            return ezyClass.getName() + "(" + fieldsStr + ")";
        }
        return ezyClass.getName() + "@" + Integer.toHexString(System.identityHashCode(this));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof EzyInstance other)) return false;
        if (ezyClass.hasDecorator("data") || ezyClass.hasDecorator("equals")) {
            return ezyClass == other.ezyClass && fields.equals(other.fields);
        }
        return this == obj;
    }

    @Override
    public int hashCode() {
        if (ezyClass.hasDecorator("data") || ezyClass.hasDecorator("equals")) {
            return Objects.hash(ezyClass, fields);
        }
        return System.identityHashCode(this);
    }

    public EzyInstance copy() {
        EzyInstance copy = new EzyInstance(ezyClass);
        copy.fields.putAll(this.fields);
        return copy;
    }

    private String formatValue(Object value) {
        if (value instanceof Double d) {
            if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
                return String.valueOf(d.longValue());
            }
        }
        if (value instanceof String) return "\"" + value + "\"";
        return String.valueOf(value);
    }
}

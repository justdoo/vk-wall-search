package ru.justdoo.vk.util;

import java.util.Collection;
import java.util.Map;

public final class Text {
    private static final String delta = "    ";

    public static  String format(Object object) {
        final StringBuilder builder = new StringBuilder();
        value(object, "", builder);
        return builder.toString();
    }

    // --------------------------------------------------------------------------------------------

    private static void value(Object value, String indent, StringBuilder builder) {
        if (value instanceof Map) {
            map((Map) value, indent, builder);
        } else if (value instanceof Collection) {
            collection((Collection) value, indent, builder);
        } else {
            builder.append(value);
        }
    }

    private static void collection(Collection collection, String indent, StringBuilder builder) {
        builder.append("[\n");
        for (Object value : collection) {
            builder.append(indent);
            builder.append(delta);
            value(value, indent + delta, builder);
            builder.append("\n");
        }
        builder.append(indent);
        builder.append("]");
    }

    private static void map(Map<?, ?> map, String indent, StringBuilder builder) {
        builder.append("{\n");
        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            final Object key = entry.getKey();
            final Object value = entry.getValue();
            builder.append(indent);
            builder.append(delta);
            builder.append(key);
            builder.append(": ");
            value(value, indent + delta, builder);
            builder.append("\n");
        }
        builder.append(indent);
        builder.append("}");
    }
}

package com.zyc.copier_v0.modules.monitor.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class Mt5PositionCommentCodec {

    public static final String PREFIX = "cp1";

    private Mt5PositionCommentCodec() {
    }

    public static Map<String, Long> parseTrackingFields(String comment) {
        Map<String, Long> values = new LinkedHashMap<>();
        if (!StringUtils.hasText(comment)) {
            return values;
        }
        String[] parts = comment.split("\\|");
        if (parts.length == 0 || !PREFIX.equalsIgnoreCase(parts[0].trim())) {
            return values;
        }
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            int idx = part.indexOf('=');
            if (idx <= 0 || idx == part.length() - 1) {
                continue;
            }
            String key = part.substring(0, idx);
            String rawValue = part.substring(idx + 1);
            try {
                values.put(key, Long.parseLong(rawValue));
            } catch (NumberFormatException ignored) {
                // Ignore malformed comment fragments from terminal/broker side.
            }
        }
        return values;
    }
}

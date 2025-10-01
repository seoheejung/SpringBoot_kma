package com.example.demo.util;

public class LogMaskUtil {

    // 인스턴스화 방지: 의도치 않은 호출시 즉시 실패하도록 AssertionError 던짐
    private LogMaskUtil() {
        throw new AssertionError("LogMaskUtil is a utility class and cannot be instantiated.");
    }

    public static String mask(String value) {
        if (value == null) return null;
        int visible = Math.min(4, value.length());
        return "*".repeat(value.length() - visible) + value.substring(value.length() - visible);
    }
    public static String mask(String value, int visibleCount) {
        if (value == null || value.length() <= visibleCount) return value;
        int maskLength = value.length() - visibleCount;
        return "*".repeat(maskLength) + value.substring(maskLength);
    }

}

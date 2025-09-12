package com.example.demo.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    /** 현재 시각 (yyyyMMddHHmm) */
    public static String nowTime() {
        return LocalDateTime.now()
                .format(FORMATTER);
    }

    /** n시간 전 (yyyyMMddHHmm) */
    public static String nowMinusHours(int hours) {
        return LocalDateTime.now()
                .minusHours(hours)
                .format(FORMATTER);
    }

    /** 1시간 전 (yyyyMMddHHmm) */
    public static String nowMinusOneHour() {
        return nowMinusHours(1);
    }

    /** 문자열을 Instant로 변환 */
    public static java.time.Instant toInstant(String tm) {
        LocalDateTime ldt = LocalDateTime.parse(tm, FORMATTER);
        return ldt.atZone(SEOUL_ZONE).toInstant();
    }
}

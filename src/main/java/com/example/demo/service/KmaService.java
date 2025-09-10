package com.example.demo.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class KmaService {

    private final InfluxDBClient influxDBClient;

    @Value("${kma.base-url}")
    private String baseUrl;

    @Value("${kma.auth-key}")
    private String authKey;

    @Value("${kma.station}")
    private String station;

    private boolean initialized = false;

    @PostConstruct public void init() { 
        // 초기 적재 
        initialized = true; 
    }

    /**
     * 매 정시마다 실행 → 지난 1시간 데이터 수집 후 InfluxDB 적재
     */
    @Scheduled(cron = "0 10 * * * *") 
    public void fetchAndStoreScheduled() {
        if (initialized) {
            initialized = false;
            log.info("🚫 첫 스케줄은 초기 적재와 겹치므로 skip");
            return;
        }
        fetchAndStore(nowMinusOneHour(), nowTime());
    }

    /**
     * 📌 원하는 시간 범위를 받아서 KMA API → InfluxDB 적재
     */
    public int fetchAndStore(String tm1, String tm2) {
        int savedCount = 0;

        String url = String.format("%s?stn=%s&tm1=%s&tm2=%s&authKey=%s",
                baseUrl, station, tm1, tm2, authKey);

        log.info("🌐 KMA API 호출: {}", url);

        String response = WebClient.create()
                .get().uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (response == null || response.isBlank()) {
            log.warn("⚠️ KMA API 응답이 비어있음");
            return savedCount;
        }

        WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

        for (String line : response.split("\n")) {
            if (line.startsWith("#") || line.trim().isEmpty()) continue;

            try {
                String[] parts = line.trim().split("\\s+");

                String tm = parts[0];
                String stn = parts[1];

                double wd = parseDouble(parts[2]);
                double ws = parseDouble(parts[3]);
                double pa = parseDouble(parts[7]);
                double ta = parseDouble(parts[11]);
                double rn = parseDouble(parts[15]);

                Instant time = parseTime(tm);

                writeApi.writePoints(Arrays.asList(
                        makePoint("wind_dir", stn, wd, time),
                        makePoint("wind_speed", stn, ws, time),
                        makePoint("pressure", stn, pa, time),
                        makePoint("temperature", stn, ta, time),
                        makePoint("rainfall", stn, rn, time)
                ));
                savedCount++;
                log.info("✅ KMA 데이터 저장: time={} temp={}", time, ta);
            } catch (Exception e) {
                log.error("❌ 데이터 파싱 오류: {}", line, e);
            } 
        }

        return savedCount;
    }

    private Point makePoint(String sensor, String station, double value, Instant time) {
        return Point.measurement("sensor_data")
                .addTag("sensor", sensor)
                .addTag("station", station)
                .addField("value", value)
                .time(time, WritePrecision.NS);
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s); }
        catch (Exception e) { return Double.NaN; }
    }

    private Instant parseTime(String tm) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        LocalDateTime ldt = LocalDateTime.parse(tm, f);
        return ldt.atZone(ZoneId.of("Asia/Seoul")).toInstant();
    }

    private String nowTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
    }

    private String nowMinusOneHour() {
        return LocalDateTime.now().minusHours(1).format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
    }
}

package com.example.demo.fixture;

import com.example.demo.domain.Sensor;
import com.example.demo.repository.SensorRepository;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import com.example.demo.service.KmaService;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServerInitializationFixture {

    private final SensorRepository sensorRepository;
    private final KmaService kmaService;

    @Value("${kma.init-days:31}")
    private int init_days;

    @PostConstruct
    @Transactional(rollbackOn = { Exception.class })
    public void initializationServer() throws Exception {
        log.info("🚀 서버 실행 시 기본 Sensor 데이터 초기화 시작");

        createSensorIfNotExists("temperature", "℃", "STN_108");
        createSensorIfNotExists("wind_speed", "m/s", "STN_108");
        createSensorIfNotExists("wind_dir", "16방위", "STN_108");
        createSensorIfNotExists("pressure", "hPa", "STN_108");
        createSensorIfNotExists("rainfall", "mm", "STN_108");

        log.info("✅ 기본 Sensor 데이터 초기화 완료");

        // 📌 31일전  00시 ~ 현재 시간의 정시까지 초기 데이터 적재
        String tm1 = LocalDate.now().minusDays(init_days).atStartOfDay()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        String tm2 = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0)
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));

        int saved = kmaService.fetchAndStore(tm1, tm2);
        log.info("📊 초기 KMA 데이터 적재 완료: {}건 저장 ({} ~ {})", saved, tm1, tm2);

    }

    private Sensor createSensorIfNotExists(String name, String unit, String location) {
        return sensorRepository.findByName(name)
                .orElseGet(() -> sensorRepository.save(
                        Sensor.builder()
                            .name(name)
                            .unit(unit)
                            .location(location)
                            .build()
                ));
    }

}

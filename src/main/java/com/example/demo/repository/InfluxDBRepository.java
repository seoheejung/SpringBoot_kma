package com.example.demo.repository;

import com.example.demo.domain.SensorMeasurement;
import java.util.List;
import java.time.Instant;

public interface InfluxDBRepository {
    void save(SensorMeasurement measurement);

    // 특정 센서 (기간 제한)
    List<SensorMeasurement> findBySensorIdWithin(String bucket, String sensorName, long durationSec);

    // 전체 조회 (⚠️ 성능 위험, 개발용)
    List<SensorMeasurement> findAll(String bucket);

    // 📌 기간별 조회 (start ~ end)
    List<SensorMeasurement> findBySensorIdBetween(String bucket, String sensorName, Instant start, Instant end);

}

package com.example.demo.service;

import com.example.demo.domain.SensorMeasurement;
import com.example.demo.dto.SensorMeasurementRequest;
import com.example.demo.dto.SensorMeasurementResponse;
import com.example.demo.repository.InfluxDBRepository;
import com.example.demo.repository.SensorRepository;
import com.example.demo.domain.Sensor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeasurementService {

        private final InfluxDBRepository influxDBRepository;
        private final SensorRepository sensorRepository;

        /**
         * ✅ 수동 저장
         * - 외부 요청에서 들어온 sensorId(Long)를 Sensor 엔티티 조회
         * - Sensor.name(String)을 InfluxDB Tag로 사용
         */
        public void saveMeasurement(SensorMeasurementRequest request) {
                Sensor sensor = sensorRepository.findById(request.getSensorId())
                        .orElseThrow(() -> new IllegalArgumentException("Sensor not found"));

                SensorMeasurement measurement = new SensorMeasurement(
                        sensor.getName(),           // Tag: sensorId → Sensor.name
                        request.getValue(),
                        Instant.now()               // 저장 시각 = 현재 시각
                );
                influxDBRepository.save(measurement);
                log.info("✅ 저장 완료: sensor={} value={} time={}", sensor.getName(), request.getValue(), Instant.now());
        }

        /**
         * ✅ 조회 (sensorId 기반)
         * - sensorId(Long) → Sensor.name(String) 변환 후 InfluxDB 조회
         */
        public List<SensorMeasurementResponse> getMeasurements(Long sensorId, long durationSec) {
                Sensor sensor = sensorRepository.findById(sensorId)
                        .orElseThrow(() -> new IllegalArgumentException("Sensor not found"));

                return influxDBRepository.findBySensorIdWithin(sensor.getName(), durationSec).stream()
                        .map(m -> new SensorMeasurementResponse(
                                sensorId,
                                m.getValue(),
                                m.getSensingDate()
                        ))
                        .toList();
        }

        /**
         * ✅ 조회 (sensorName 기반)
         * - 바로 InfluxDB에서 sensorName(String)으로 검색
         * - Sensor 테이블에서 id 조회해서 응답에 포함
         */
        public List<SensorMeasurementResponse> getMeasurementsByName(String sensorName, long durationSec) {
                Long sensorId = sensorRepository.findByName(sensorName)
                        .map(Sensor::getId)
                        .orElse(null);

                return influxDBRepository.findBySensorIdWithin(sensorName, durationSec).stream()
                        .map(m -> new SensorMeasurementResponse(
                                sensorId,  // Sensor 테이블에 있으면 ID, 없으면 null
                                m.getValue(),
                                m.getSensingDate()
                        ))
                        .toList();
        }
}

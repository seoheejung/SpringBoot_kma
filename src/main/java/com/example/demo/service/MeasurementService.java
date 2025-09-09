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
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.constants.HttpStatusCodeContrants;
import org.springframework.beans.factory.annotation.Value;
import java.util.concurrent.CompletableFuture;


@Slf4j
@Service
@RequiredArgsConstructor
public class MeasurementService {

        private final InfluxDBRepository influxDBRepository;
        private final SensorRepository sensorRepository;

        @Value("${influx.bucket}")
        private String bucket;

        /**
         * ✅ 수동 저장
         * - 외부 요청에서 들어온 sensorId(Long)를 Sensor 엔티티 조회
         * - Sensor.name(String)을 InfluxDB Tag로 사용
         */
        public int saveMeasurement(SensorMeasurementRequest request) throws Exception {
                Sensor sensor = sensorRepository.findById(request.getSensorId())
                        .orElseThrow(() -> new IllegalArgumentException("Sensor not found"));

                Instant now = Instant.now();
                SensorMeasurement measurement = new SensorMeasurement(
                        sensor.getName(),           // Tag: sensorId → Sensor.name
                        request.getValue(),
                        now               // 저장 시각 = 현재 시각
                );
                influxDBRepository.save(measurement);
                log.info("✅ 저장 완료: sensor={} value={} time={}", sensor.getName(), request.getValue(), now );

                return HttpStatusCodeContrants.OK;
        }


        /**
         * ✅ 조회 (sensorId 기반)
         * - sensorId(Long) → Sensor.name(String) 변환 후 InfluxDB 조회
         */
        @Transactional(readOnly=true)
        public List<SensorMeasurementResponse> getMeasurements(Long sensorId, long durationSec) {
                Sensor sensor = sensorRepository.findById(sensorId)
                        .orElseThrow(() -> new IllegalArgumentException("Sensor not found: id=" + sensorId));

                return influxDBRepository.findBySensorIdWithin(bucket, sensor.getName(), durationSec).stream()
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
                        .orElseThrow(() -> new IllegalArgumentException("Sensor not found: name=" + sensorName));

                return influxDBRepository.findBySensorIdWithin(bucket, sensorName, durationSec).stream()
                        .map(m -> new SensorMeasurementResponse(
                                sensorId,
                                m.getValue(),
                                m.getSensingDate()
                        ))
                        .toList();
        }

        @Transactional(readOnly = true)
        public List<SensorMeasurementResponse> getAllMeasurements() {
                return influxDBRepository.findAll(bucket).stream()
                        .map(m -> {
                                Long sensorId = sensorRepository.findByName(m.getSensorId())
                                        .map(Sensor::getId)
                                        .orElse(null);
                                return new SensorMeasurementResponse(
                                        sensorId,
                                        m.getValue(),
                                        m.getSensingDate()
                                );
                        })
                        .toList();
        }

        @Transactional(readOnly = true)
        public List<SensorMeasurementResponse> getAllWithinMeasurements(long durationSec) {
                return influxDBRepository.findAllWithin(bucket, durationSec).stream()
                        .map(m -> {
                                Long sensorId = sensorRepository.findByName(m.getSensorId())
                                        .map(Sensor::getId)
                                        .orElse(null);
                                return new SensorMeasurementResponse(
                                        sensorId,
                                        m.getValue(),
                                        m.getSensingDate()
                                );
                        })
                        .toList();
        }

        @Transactional(readOnly = true)
        public Map<String, List<SensorMeasurementResponse>> getAllMeasurementsGroupedBySensor(long durationSec) {
                List<Sensor> sensors = sensorRepository.findAll();

                // 각 센서별로 InfluxDB에서 최근 durationSec 동안의 데이터를 조회하여 Map으로 그룹핑
                return sensors.stream()
                        .collect(Collectors.toMap(
                                Sensor::getName, // key → 센서 이름 (예: "temperature", "wind_speed")

                                // value → 비동기적으로 InfluxDB 조회 후 Response 변환
                                sensor -> CompletableFuture.supplyAsync(() ->
                                        influxDBRepository.findBySensorIdWithin(bucket, sensor.getName(), durationSec).stream()
                                                // InfluxDB에서 조회한 SensorMeasurement → SensorMeasurementResponse 변환
                                                .map(m -> new SensorMeasurementResponse(
                                                        sensor.getId(),
                                                        m.getValue(),  
                                                        m.getSensingDate()  
                                                ))
                                                .toList()
                                ).join() // Future 결과를 기다려서 List<SensorMeasurementResponse> 반환
                        ));
        }

}

package com.example.demo.service;

import com.example.demo.domain.SensorMeasurement;
import com.example.demo.dto.SensorMeasurementRequest;
import com.example.demo.dto.SensorMeasurementResponse;
import com.example.demo.repository.InfluxDBRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApiService {

    private final InfluxDBRepository influxDBRepository;

    public void saveMeasurement(SensorMeasurementRequest request) {
        SensorMeasurement measurement = new SensorMeasurement(
            request.getSensorId().toString(),   // ✅ String 변환
            request.getValue(),
            Instant.now()
        );
        influxDBRepository.save(measurement);
    }

    public List<SensorMeasurementResponse> getMeasurements(Long sensorId, long durationSec) {
        return influxDBRepository.findBySensorIdWithin(sensorId, durationSec).stream()
                .map(m -> new SensorMeasurementResponse(
                        Long.valueOf(m.getSensorId()),  // ✅ 다시 Long으로 변환
                        m.getValue(),
                        m.getSensingDate()
                ))
                .toList();
    }
}

package com.example.demo.repository;

import com.example.demo.domain.SensorMeasurement;
import java.util.List;

public interface InfluxDBRepository {
    void save(SensorMeasurement measurement);
    List<SensorMeasurement> findBySensorIdWithin(Long sensorId, long durationSec);
}

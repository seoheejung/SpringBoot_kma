package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.Instant;

@Data
@AllArgsConstructor
public class SensorMeasurementResponse {
    private Long sensorId;      // MariaDB Sensor 엔티티의 PK
    private Double value;       // 시계열 값
    private Instant sensingDate; // 관측 시각 (UTC 변환)
}

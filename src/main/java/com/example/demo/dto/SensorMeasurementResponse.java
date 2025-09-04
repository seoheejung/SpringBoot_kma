package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.Instant;

@Data
@AllArgsConstructor
public class SensorMeasurementResponse {
    private Long sensorId;
    private Double value;
    private Instant sensingDate;
}

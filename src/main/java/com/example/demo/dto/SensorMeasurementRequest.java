package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor   // JSON 역직렬화용
@AllArgsConstructor
public class SensorMeasurementRequest {
    private Long sensorId;
    private Double value;
}

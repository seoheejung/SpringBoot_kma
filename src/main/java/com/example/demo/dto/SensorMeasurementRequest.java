package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor   // JSON 역직렬화용 (Spring MVC가 body → 객체 변환 시 필요)
@AllArgsConstructor
public class SensorMeasurementRequest {
    private Long sensorId;   // MariaDB Sensor 엔티티의 PK
    private Double value;    // 저장할 측정값
}

package com.example.demo.domain;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Measurement(name = "sensor_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorMeasurement {

    @Column(tag = true, name = "sensor")
    private String sensorId;   // ✅ Long → String

    @Column
    private Double value;

    @Column(name = "_time", timestamp = true) // ✅ 실제 데이터 시각
    private Instant sensingDate;
}

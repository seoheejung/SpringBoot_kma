package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sensors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;      // 예: temperature, wind_speed, wind_dir, pressure, rainfall

    @Column(nullable = false)
    private String unit;      // 예: ℃, m/s, hPa, mm

    @Column(nullable = false)
    private String location;  // 예: STN_108 (지점번호)
}

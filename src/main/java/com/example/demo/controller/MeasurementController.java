package com.example.demo.controller;

import com.example.demo.dto.SensorMeasurementRequest;
import com.example.demo.dto.SensorMeasurementResponse;
import com.example.demo.service.MeasurementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/measurements")
@RequiredArgsConstructor
public class MeasurementController {

    private final MeasurementService measurementService;

    /**
     * ✅ 수동 저장 API
     * POST /api/measurements
     */
    @PostMapping
    public String saveMeasurement(@RequestBody SensorMeasurementRequest request) {
        measurementService.saveMeasurement(request);
        return "Measurement saved!";
    }

    /**
     * ✅ 조회 API (sensorId 기반)
     * GET /api/measurements/{sensorId}?durationSec=3600
     */
    @GetMapping("/{sensorId}")
    public List<SensorMeasurementResponse> getMeasurementsById(
            @PathVariable Long sensorId,
            @RequestParam(defaultValue = "3600") long durationSec
    ) {
        return measurementService.getMeasurements(sensorId, durationSec);
    }

    /**
     * ✅ 조회 API (sensorName 기반)
     * GET /api/measurements/by-name/{sensorName}?durationSec=3600
     */
    @GetMapping("/by-name/{sensorName}")
    public List<SensorMeasurementResponse> getMeasurementsByName(
            @PathVariable String sensorName,
            @RequestParam(defaultValue = "3600") long durationSec
    ) {
        return measurementService.getMeasurementsByName(sensorName, durationSec);
    }
}

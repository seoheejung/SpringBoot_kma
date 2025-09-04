package com.example.demo.controller;

import com.example.demo.dto.SensorMeasurementRequest;
import com.example.demo.dto.SensorMeasurementResponse;
import com.example.demo.service.ApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/measurements")
@RequiredArgsConstructor
public class ApiController {

    private final ApiService apiService;

    @PostMapping
    public String saveMeasurement(@RequestBody SensorMeasurementRequest request) {
        apiService.saveMeasurement(request);
        return "Measurement saved!";
    }

    @GetMapping("/{sensorId}")
    public List<SensorMeasurementResponse> getMeasurements(
            @PathVariable Long sensorId,
            @RequestParam(defaultValue = "60") long durationSec
    ) {
        return apiService.getMeasurements(sensorId, durationSec);
    }
}
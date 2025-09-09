package com.example.demo.controller;

import com.example.demo.dto.AdminResponse;
import com.example.demo.dto.SensorMeasurementRequest;
import com.example.demo.dto.SensorMeasurementResponse;
import com.example.demo.service.MeasurementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
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
public AdminResponse<?> saveMeasurement(@RequestBody SensorMeasurementRequest request) throws Exception {
    int status = measurementService.saveMeasurement(request);
    return AdminResponse.builder()
            .status(status) 
            .build();
}
    /**
     * ✅ 조회 API (sensorId 기반)
     * GET /api/measurements/{sensorId}?durationSec=86400
     */
    @GetMapping("/{sensorId}")
    public AdminResponse<List<SensorMeasurementResponse>> getMeasurementsById(
            @PathVariable Long sensorId,
            @RequestParam(defaultValue = "86400") long durationSec
    ) {
        List<SensorMeasurementResponse> list = measurementService.getMeasurements(sensorId, durationSec);

        return AdminResponse.<List<SensorMeasurementResponse>>builder()
                .payload(list)
                .build();
    }

    /**
     * ✅ 조회 API (sensorName 기반)
     * GET /api/measurements/by-name/{sensorName}?durationSec=86400
     */
    @GetMapping("/by-name/{sensorName}")
    public AdminResponse<List<SensorMeasurementResponse>> getMeasurementsByName(
            @PathVariable String sensorName,
            @RequestParam(defaultValue = "86400") long durationSec
    ) {
        List<SensorMeasurementResponse> list = measurementService.getMeasurementsByName(sensorName, durationSec);
        return AdminResponse.<List<SensorMeasurementResponse>>builder()
            .payload(list)
            .build();
    }

    /**
     * ✅ 전체 조회 (⚠️ 성능 주의)
     */
    @GetMapping("/all")
    public AdminResponse<List<SensorMeasurementResponse>> getAllMeasurements() {
        List<SensorMeasurementResponse> list = measurementService.getAllMeasurements();
        return AdminResponse.<List<SensorMeasurementResponse>>builder()
                .payload(list)
                .build();
    }

    /**
     * ✅ 전체 조회 (기간 제한)
     */
    @GetMapping("/all/within")
    public AdminResponse<List<SensorMeasurementResponse>> getAllWithinMeasurements(
            @RequestParam(defaultValue = "86400") long durationSec
    ) {
        List<SensorMeasurementResponse> list = measurementService.getAllWithinMeasurements(durationSec);
        return AdminResponse.<List<SensorMeasurementResponse>>builder()
                .payload(list)
                .build();
    }

    /**
     * ✅ 전체 조회 (센서별 그룹핑)
     */
    @GetMapping("/all/grouped")
    public AdminResponse<Map<String, List<SensorMeasurementResponse>>> getAllMeasurementsGrouped(
            @RequestParam(defaultValue = "86400") long durationSec
    ) {
        Map<String, List<SensorMeasurementResponse>> grouped = measurementService.getAllMeasurementsGroupedBySensor(durationSec);
        return AdminResponse.<Map<String, List<SensorMeasurementResponse>>>builder()
                .payload(grouped)
                .build();
    }

}

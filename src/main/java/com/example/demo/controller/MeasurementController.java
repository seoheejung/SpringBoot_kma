package com.example.demo.controller;

import com.example.demo.dto.AdminResponse;
import com.example.demo.dto.SensorMeasurementRequest;
import com.example.demo.dto.SensorMeasurementResponse;
import com.example.demo.service.MeasurementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;

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
         * ✅ 기간 조회 (오프셋 없는 문자열 → 한국시간 처리)
         */
        @GetMapping("/list")
        public List<SensorMeasurementResponse> getMeasurementsBetween(
                @RequestParam String sensorName,
                @RequestParam("start") 
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                @RequestParam("end") 
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

                // 📌 LocalDateTime을 Asia/Seoul 기준으로 Instant 변환
                Instant startInstant = start.atZone(ZoneId.of("Asia/Seoul")).toInstant();
                Instant endInstant   = end.atZone(ZoneId.of("Asia/Seoul")).toInstant();

                return measurementService.getMeasurementsBetween(sensorName, startInstant, endInstant);
        }

        /**
         * ✅ 기간 조회 (센서별 그룹핑)
         */
        @GetMapping("/list/grouped")
        public AdminResponse<Map<String, List<SensorMeasurementResponse>>> getMeasurementsGrouped(
                @RequestParam("start") 
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                @RequestParam("end") 
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        Instant startInstant = start.atZone(ZoneId.of("Asia/Seoul")).toInstant();
        Instant endInstant   = end.atZone(ZoneId.of("Asia/Seoul")).toInstant();

        Map<String, List<SensorMeasurementResponse>> grouped =
                measurementService.getMeasurementsGroupedBySensor(startInstant, endInstant);

        return AdminResponse.<Map<String, List<SensorMeasurementResponse>>>builder()
                .payload(grouped)
                .build();
        }


}

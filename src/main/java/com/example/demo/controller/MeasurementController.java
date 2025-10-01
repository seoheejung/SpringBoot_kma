package com.example.demo.controller;

import com.example.demo.dto.AdminResponse;
import com.example.demo.dto.SensorMeasurementRequest;
import com.example.demo.dto.SensorMeasurementResponse;
import com.example.demo.service.MeasurementService;
import com.example.demo.util.LogMaskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.*;
import java.util.List;
import java.util.Map;

@Slf4j
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
                String maskedName = LogMaskUtil.mask(String.valueOf(request.getSensorId()));
                log.info("Measurement 저장 요청: sensorName={}", maskedName);

                int status = measurementService.saveMeasurement(request);

                log.info("✅ Measurement 저장 완료: sensorName={}, status={}", maskedName, status);

                return AdminResponse.builder().status(status).build();
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
                log.info("Measurement 조회 byId: sensorId={}, durationSec={}", sensorId, durationSec);

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
                String maskedName = LogMaskUtil.mask(sensorName);
                log.info("Measurement 조회 byName: sensorName={}, durationSec={}", maskedName, durationSec);

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
                log.info("Measurement 조회: 전체");

                List<SensorMeasurementResponse> list = measurementService.getAllMeasurements();

                return AdminResponse.<List<SensorMeasurementResponse>>builder()
                        .payload(list)
                        .build();
        }

        /**
         * ✅ 기간 조회 (오프셋 없는 문자열 → 한국시간 처리)
         */
        @GetMapping("/list")
        public AdminResponse<List<SensorMeasurementResponse>> getMeasurementsBetween(
                @RequestParam String sensorName,
                @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
        ) {
                String maskedName = LogMaskUtil.mask(sensorName);
                Instant startInstant = start.atZone(ZoneId.of("Asia/Seoul")).toInstant();
                Instant endInstant = end.atZone(ZoneId.of("Asia/Seoul")).toInstant();

                log.info("Measurement 조회 기간: sensorName={}, start={}, end={}", maskedName, start, end);

                List<SensorMeasurementResponse> list = measurementService.getMeasurementsBetween(sensorName, startInstant, endInstant);

                return AdminResponse.<List<SensorMeasurementResponse>>builder()
                        .payload(list)
                        .build();
        }

        /**
         * ✅ 기간 조회 (센서별 그룹핑)
         */
        @GetMapping("/list/grouped")
        public AdminResponse<Map<String, List<SensorMeasurementResponse>>> getMeasurementsGrouped(
                @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
        ) {
                Instant startInstant = start.atZone(ZoneId.of("Asia/Seoul")).toInstant();
                Instant endInstant = end.atZone(ZoneId.of("Asia/Seoul")).toInstant();

                log.info("Measurement 그룹 조회: start={}, end={}", start, end);

                Map<String, List<SensorMeasurementResponse>> grouped = measurementService.getMeasurementsGroupedBySensor(startInstant, endInstant);

                return AdminResponse.<Map<String, List<SensorMeasurementResponse>>>builder()
                        .payload(grouped)
                        .build();
        }


}

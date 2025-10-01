package com.example.demo.controller;

import com.example.demo.dto.AdminResponse;
import com.example.demo.service.ForecastSummaryService;
import com.example.demo.util.LogMaskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Pattern;

@Slf4j
@RestController
@RequestMapping("/api/forecast")
@RequiredArgsConstructor
public class ForecastSummaryController {

    private final ForecastSummaryService service;

    @PostMapping
    public AdminResponse<?> fetchForecast(
            @RequestParam @Pattern(regexp = "\\d{10,12}", message = "tm1 must be 10~12 digits") String tm1,
            @RequestParam @Pattern(regexp = "\\d{10,12}", message = "tm2 must be 10~12 digits") String tm2
    ) throws Exception {
        String maskedTm1 = LogMaskUtil.mask(tm1);
        String maskedTm2 = LogMaskUtil.mask(tm2);

        log.info("ForecastSummary 호출: tm1={}, tm2={}", maskedTm1, maskedTm2);

        int status = service.fetchAndSave(tm1, tm2);

        log.info("✅ ForecastSummary 처리 완료: tm1={}, tm2={}, status={}", maskedTm1, maskedTm2, status);

        return AdminResponse.builder()
                .status(status)
                .build();
    }
}

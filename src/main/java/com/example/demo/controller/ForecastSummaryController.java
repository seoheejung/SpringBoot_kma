package com.example.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.example.demo.dto.AdminResponse;
import com.example.demo.service.ForecastSummaryService;

@RestController
@RequestMapping("/api/forecast")
@RequiredArgsConstructor
public class ForecastSummaryController {

    private final ForecastSummaryService service;

    /**
     * 단기예보 개황 수집 & DB 저장
     * 호출 예: POST /api/forecast?tmfc1=2025091106&tmfc2=2025091118
     */
    @PostMapping
    public AdminResponse<?> fetchForecast( @RequestParam String tmfc1, @RequestParam String tmfc2) throws Exception {
        int status = service.fetchAndSave(tmfc1, tmfc2);
        return AdminResponse.builder()
                .status(status) 
                .build();
    }
}


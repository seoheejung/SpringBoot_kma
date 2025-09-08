package com.example.demo.controller;

import com.example.demo.service.KmaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.example.demo.dto.AdminResponse;
import com.example.demo.constants.HttpStatusCodeContrants;


@RestController
@RequestMapping("/api/kma")
@RequiredArgsConstructor
@Slf4j
public class KmaController {

    private final KmaService kmaService;

    /**
     * ✅ 특정 기간 데이터를 수집 & InfluxDB 저장
     */
    @PostMapping("/fetch")
    public AdminResponse<String> fetchAndStore(
            @RequestParam String tm1,
            @RequestParam String tm2
    ) throws Exception {
        int savedCount = kmaService.fetchAndStore(tm1, tm2);
        return AdminResponse.<String>builder()
                .payload("Saved count: " + savedCount)
                .build();
    }
}

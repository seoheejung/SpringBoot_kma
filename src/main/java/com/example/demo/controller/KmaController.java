package com.example.demo.controller;

import com.example.demo.service.KmaService;
import com.example.demo.dto.AdminResponse;
import com.example.demo.util.LogMaskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/kma")
@RequiredArgsConstructor
@Slf4j
public class KmaController {

    private final KmaService kmaService;

    /**
     * KMA 데이터를 조회하고 DB에 저장
     * tm1, tm2는 10~12자리 숫자여야 함
     */
    @PostMapping("/fetch")
    public AdminResponse<String> fetchAndStore(
            @RequestParam @Pattern(regexp = "\\d{10,12}", message = "tm1 must be 10~12 digits") String tm1,
            @RequestParam @Pattern(regexp = "\\d{10,12}", message = "tm2 must be 10~12 digits") String tm2
    ) throws Exception {

        // 로그 마스킹 + 안전하게 로그 남기기
        String maskedTm1 = LogMaskUtil.mask(tm1);
        String maskedTm2 = LogMaskUtil.mask(tm2);
        log.info("KMA API 호출 시작 tm1={}, tm2={}", maskedTm1, maskedTm2);

        int savedCount = kmaService.fetchAndStore(tm1, tm2);

        log.info("KMA API 호출 완료. 저장 건수: {}", savedCount);

        return AdminResponse.<String>builder()
                .payload("Saved count: " + savedCount)
                .build();
    }
}

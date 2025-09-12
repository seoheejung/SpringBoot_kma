package com.example.demo.service;

import com.example.demo.constants.HttpStatusCodeContrants;
import com.example.demo.domain.ForecastSummary;
import com.example.demo.repository.ForecastSummaryRepository;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import static com.example.demo.util.TimeUtils.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForecastSummaryService {

    private final ForecastSummaryRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();

    // ✅ 따옴표 없는 JSON 필드 허용
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    @Value("${kma.fct-url}")
    private String fctUrl;

    @Value("${kma.auth-key}")
    private String authKey;

    @Value("${kma.station}")
    private String station;

    /**
     * 매 6시간마다 실행 (0시, 6시, 12시, 18시 정각에 실행)
     */
    @Scheduled(cron = "0 0 */6 * * *")
    public void fetchAndStoreScheduled() {
        String tm2 = nowTime();
        String tm1 = nowMinusHours(6);

        log.info("⏰ 스케줄 실행: {} ~ {}", tm1, tm2);
        fetchAndSave(tm1, tm2);
    }


    /**
     * 기상청 단기예보 개황 데이터 조회 + 저장
     */
    @Transactional
    public int fetchAndSave(String tmfc1, String tmfc2) {
        String url = String.format(
            "%s?stn=%s&tmfc1=%s&tmfc2=%s&disp=1&authKey=%s",
            fctUrl, station, tmfc1, tmfc2, authKey
        );
        log.info("🌐 KMA API 호출: {}", url);

        try {
            String response = restTemplate.getForObject(url, String.class);
            if (response == null || response.isBlank()) {
                return HttpStatusCodeContrants.FORCE_ERROR;
            }

            // log.debug("🔍 응답 데이터 (앞부분): {}", response.substring(0, Math.min(300, response.length())));

            // ✅ #START7777, #7777END 제거
            String cleaned = response
                    .replaceAll("(?s)#START7777", "")
                    .replaceAll("#7777END", "")
                    .trim();

            // log.debug("🧹 정리된 응답 (앞부분): {}", cleaned.substring(0, Math.min(300, cleaned.length())));

            // ✅ JSON 파싱
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode dataArray = root.get("fct_afs_ds");
            if (dataArray == null || !dataArray.isArray() || dataArray.isEmpty()) {
                return HttpStatusCodeContrants.NON_AUTHORITATIVE_INFO;
            }

            DateTimeFormatter jsonFmt = DateTimeFormatter.ofPattern("yyyy.MM.dd.HH:mm");

            int count = 0;
            for (JsonNode node : dataArray) {
                ForecastSummary summary = ForecastSummary.builder()
                        .stnId(node.get("stn_id").asInt())
                        .tmFc(LocalDateTime.parse(node.get("tm_fc").asText(), jsonFmt))
                        .manFcId(node.hasNonNull("man_fc_id") ? node.get("man_fc_id").asText() : null)
                        .manFc(node.hasNonNull("man_fc") ? node.get("man_fc").asText() : null)
                        .cnt(node.hasNonNull("cnt") ? node.get("cnt").asInt() : null)
                        .wfSv1(node.path("wf_sv1").asText(null))
                        .wfSv2(node.path("wf_sv2").asText(null))
                        .wfSv3(node.path("wf_sv3").asText(null))
                        .wn(node.path("wn").asText(null))
                        .wr(node.path("wr").asText(null))
                        .rem(node.path("rem").asText(null))
                        .build();

                repository.upsert(summary);
                count++;
            }
            log.info("✅ JSON 형식 {}건 저장 완료", count);

            return HttpStatusCodeContrants.OK;
        } catch (Exception e) {
            log.error("❌ KMA 단기예보 개황 데이터 처리 오류", e);
            return HttpStatusCodeContrants.FORCE_ERROR;
        }
    }

}

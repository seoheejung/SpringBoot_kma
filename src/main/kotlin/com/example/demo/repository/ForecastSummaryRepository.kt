package com.example.demo.repository

import com.example.demo.domain.ForecastSummary
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime
import org.springframework.data.repository.query.Param

interface ForecastSummaryRepository : JpaRepository<ForecastSummary, Long> {

    /**
     * 특정 관서(stnId)의 발표 시각 범위 조회
     */
    fun findByStnIdAndTmFcBetween(
        stnId: Int,
        from: LocalDateTime,
        to: LocalDateTime
    ): List<ForecastSummary>

    /**
     * ✅ Upsert (tm_fc + stn_id 기준)
     * - @Param + Named Parameter 사용
     * - Native Query 그대로 두되, 안전한 바인딩 보장
     */
    @Modifying
    @Query("""
        INSERT INTO forecast_summary (
            tm_fc, stn_id, man_fc, wf_sv1, wf_sv2, wf_sv3, wn, wr, rem
            ) VALUES (
                :#{#fs.tmFc}, :#{#fs.stnId}, :#{#fs.manFc}, :#{#fs.wfSv1}, 
                :#{#fs.wfSv2}, :#{#fs.wfSv3}, :#{#fs.wn}, :#{#fs.wr}, :#{#fs.rem}
            )
            ON DUPLICATE KEY UPDATE
                man_fc = VALUES(man_fc),
                wf_sv1 = VALUES(wf_sv1),
                wf_sv2 = VALUES(wf_sv2),
                wf_sv3 = VALUES(wf_sv3),
                wn = VALUES(wn),
                wr = VALUES(wr),
                rem = VALUES(rem)
    """, nativeQuery = true)
    fun upsert(@Param("fs") fs: ForecastSummary)


}

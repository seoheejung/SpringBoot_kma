package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "forecast_summary",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_tmfc_stn", columnNames = {"tmFc", "stnId"})
    }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForecastSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime tmFc;    // 발표시각
    private Integer stnId;         // 발표관서
    private LocalDateTime tmIn;    // 입력시각
    private Integer cnt;           // 참조번호

    private String manFc;          // 예보관명
    private String manIn;          // 입력자명
    private String manIp;          // 입력장비 IP
    private String manFcId;        // 예보관 ID
    private String manInId;        // 입력자 ID

    @Lob private String wfSv1;     // 기상개황(오늘)
    @Lob private String wfSv2;     // 기상개황(내일)
    @Lob private String wfSv3;     // 기상개황(모레)
    @Lob private String wn;        // 특보사항
    @Lob private String wr;        // 예비특보
    @Lob private String rem;       // 비고

    @Column(updatable = false, insertable = false,
            columnDefinition = "timestamp default current_timestamp")
    private LocalDateTime createdAt;
}

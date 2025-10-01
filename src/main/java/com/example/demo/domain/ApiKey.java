package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "api_keys")
@Getter @Setter
@NoArgsConstructor 
@AllArgsConstructor
@Data
@Builder
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 내부 식별자

    @Column(nullable = false, unique = true, length = 128)
    private String apiKey; // 실제 API 키 값

    @Column(nullable = false, length = 100)
    private String owner; // 소유자

    @Column(nullable = false)
    private Integer limitPerMinute; // 1분당 요청 허용량

    @Column(nullable = false)
    private Boolean active; // 사용 가능 여부
}

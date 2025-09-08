package com.example.demo.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.constants.HttpStatusCodeContrants;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminResponse<T> {

    @Builder.Default
    private int status = HttpStatusCodeContrants.OK;
    private T payload;

    public String toJson() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{}";
        }
    }

    // ✅ 헬퍼 메서드 (코드 가독성 ↑)
    public static <T> AdminResponse<T> ok(T payload) {
        return AdminResponse.<T>builder()
                .status(HttpStatusCodeContrants.OK)
                .payload(payload)
                .build();
    }

    public static <T> AdminResponse<T> error(int status, T payload) {
        return AdminResponse.<T>builder()
                .status(status)
                .payload(payload)
                .build();
    }
}

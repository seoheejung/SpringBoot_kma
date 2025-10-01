package com.example.demo.constants;

import java.io.Serializable;

public class HttpStatusCodeConstants implements Serializable {

    private static final long serialVersionUID = -3831571020204650046L;

    // 요청 성공
    public static final int OK = 200;

    // 인증 실패 (Unauthorized)
    public static final int AUTHENTICATION_FAILURE = 401;

    // 권한 없음 (Forbidden)
    public static final int FORBIDDEN = 403;

    // 요청이 너무 많음 (Too Many Requests)
    public static final int TOO_MANY_REQUESTS = 429;

    // 파라메터 실패
    public static final int ERROR_TO_PARAMETER_ERROR = 406;

    // 중복 데이터
    public static final int ERROR_DUPLICATE_ENTRY = 409;

    // 유효성 검사 실패
    public static final int ERROR_VALIDATION_FAILED = 422;

    // 성공이나 특정 값이 없음
    public static final int NON_AUTHORITATIVE_INFO = 204;

    // 강제 에러
    public static final int FORCE_ERROR = 500;
    public static final int ERROR_PROCESS_NOT_COMPLETED = 501;
    public static final int FORCE_ERROR_PROCEEDING_PROCESS = 502;
    public static final int FORCE_ERROR_COMPLETE = 503;
    public static final int FORCE_ERROR_VALIDATION_ERROR = 504;
    public static final int FORCE_ERROR_TIMEOUT = 505;
    public static final int FORCE_ERROR_PROCESSING = 506;
}

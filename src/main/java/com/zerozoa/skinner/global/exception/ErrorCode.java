package com.zerozoa.skinner.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    //Member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "사용자를 찾을 수 없습니다."),
    //중복은 Conflict(409)
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "M002", "이미 가입된 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "M003", "이미 존재하는 닉네임입니다."),

    //Auth (인증/권한)
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A001", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "만료된 토큰입니다."),
    //401(비로그인)과 403(권한없음)은 엄격히 구분
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A003", "접근 권한이 없습니다."),

    //INGREDIENT (성분)
    INGREDIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "I001", "해당 성분 정보를 찾을 수 없습니다."),

    //Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

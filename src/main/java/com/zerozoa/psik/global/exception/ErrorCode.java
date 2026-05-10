package com.zerozoa.psik.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    //Member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "사용자를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "M002", "이미 가입된 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "M003", "이미 존재하는 닉네임입니다."),
    PROFILE_ALREADY_COMPLETE(HttpStatus.CONFLICT, "M004", "이미 프로필 설정이 완료된 사용자입니다."),

    //Auth (인증/권한)
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A001", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "만료된 토큰입니다."),
    //401(비로그인)과 403(권한없음)은 엄격히 구분
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A003", "접근 권한이 없습니다."),

    //INGREDIENT (성분)
    INGREDIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "I001", "해당 성분 정보를 찾을 수 없습니다."),

    //PRODUCT (제품)
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "해당 제품 정보를 찾을 수 없습니다."),
    ALREADY_OWNED_PRODUCT(HttpStatus.CONFLICT, "T002", "이미 샀어요를 누른 제품입니다."),

    //Skin Diary (피부 일기)
    DIARY_ALREADY_EXISTS(HttpStatus.CONFLICT, "D001", "해당 날짜에 이미 작성된 다이어리가 존재합니다."),
    DIARY_NOT_FOUND(HttpStatus.NOT_FOUND, "D002", "해당 다이어리를 찾을 수 없습니다."),
    ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "D003", "해당 피부 분석 결과를 찾을 수 없습니다."),
    FACE_NOT_DETECTED(HttpStatus.BAD_REQUEST, "D004", "얼굴이 감지되지 않았습니다. 얼굴 사진을 업로드해주세요."),
    ANALYSIS_ALREADY_EXISTS(HttpStatus.CONFLICT, "D005", "이미 분석이 완료된 다이어리입니다."),
    ANALYSIS_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "D006", "하루 분석 횟수를 초과했습니다."),
    GEMINI_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS,"D007", "AI 분석 요청이 너무 많습니다. 1분 후 다시 시도해주세요."),

    // Post (게시글)
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "해당 게시글을 찾을 수 없습니다."),

    //Comment (댓글)
    COMMENT_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "CM001", "해당 대댓글의 대댓글을 작성할 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CM002", "해당 댓글을 찾을 수 없습니다."),

    //Image (이미지)
    IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "IMG001", "이미지는 최대 5장까지 첨부할 수 있습니다."),

    //Inquiry (문의하기)
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "IQ001", "해당 문의를 찾을 수 없습니다."),
    INQUIRY_ALREADY_ANSWERED(HttpStatus.CONFLICT, "IQ002", "이미 답변이 등록된 문의입니다."),

    //Common (공통)
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C003", "지원하지 않는 요청 방식입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
